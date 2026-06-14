package com.cbc_terminal_ballistics.client;

import com.cbc_terminal_ballistics.ballistics.TBCaliber;
import com.cbc_terminal_ballistics.network.ClientboundArmorSparkPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = "cbc_terminal_ballistics", value = Dist.CLIENT)
public final class ClientArmorSparkVisuals {
    private static final int MAX_ACTIVE_SPARKS = 192;
    private static final List<Spark> SPARKS = new ArrayList<>();

    public static void accept(ClientboundArmorSparkPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 origin = packet.origin();
        Vec3 direction = packet.direction();
        if (direction.lengthSqr() < 1.0e-6D) direction = new Vec3(0, 1, 0);

        SableClientCompat.RenderTransform transform = SableClientCompat.renderTransformWithSubLevel((ClientLevel) mc.level, packet.pos(), Vec3.ZERO);
        if (transform != null) {
            origin = transform.position(origin);
            direction = transform.normal(direction);
        }

        direction = direction.normalize();
        RandomSource random = RandomSource.create(packet.seed());
        TBCaliber caliber = packet.caliber();
        float intensity = Mth.clamp(packet.intensity(), 0.55F, 2.75F);
        int count = Mth.clamp(9 + caliber.ordinal() * 2 + (int) (intensity * 5.0F), 9, 28);
        long now = mc.level.getGameTime();

        for (int i = 0; i < count; i++) {
            Vec3 sparkDir = scatterDirection(direction, random);
            double length = (0.65D + random.nextDouble() * 1.45D + caliber.ordinal() * 0.18D) * (0.75D + intensity * 0.18D);
            double speed = length / (4.0D + random.nextDouble() * 3.0D);
            int lifetime = Mth.clamp(5 + random.nextInt(4) + caliber.ordinal() / 2, 5, 10);
            float width = (float) Mth.clamp(0.010D + random.nextDouble() * 0.016D + intensity * 0.004D, 0.010D, 0.040D);
            SPARKS.add(new Spark(origin, sparkDir, 0.04D + random.nextDouble() * 0.10D, length, speed, width, now, lifetime, intensity));
        }
        trimActiveSparks();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            SPARKS.clear();
            return;
        }

        long now = mc.level.getGameTime();
        SPARKS.removeIf(spark -> now - spark.startTick() >= spark.lifetimeTicks());
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || SPARKS.isEmpty()) return;

        long now = mc.level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());

        Iterator<Spark> iterator = SPARKS.iterator();
        while (iterator.hasNext()) {
            Spark spark = iterator.next();
            long ageTicks = now - spark.startTick();
            if (ageTicks < 0 || ageTicks >= spark.lifetimeTicks()) {
                iterator.remove();
                continue;
            }
            float age = ageTicks + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            renderSpark(buffer, matrix, spark, camera, age);
        }

        buffers.endBatch(RenderType.lightning());
    }

    private static void renderSpark(VertexConsumer buffer, Matrix4f matrix, Spark spark, Vec3 camera, float age) {
        float progress = age / Math.max(1.0F, spark.lifetimeTicks());
        float alpha = (1.0F - progress) * Mth.clamp(0.75F + spark.intensity() * 0.12F, 0.70F, 1.0F);
        if (alpha <= 0.01F) return;

        double headDistance = Mth.clamp(spark.startDistance() + spark.speed() * age, spark.startDistance(), spark.maxDistance());
        double tailDistance = Mth.clamp(headDistance - 0.28D - spark.maxDistance() * 0.18D, spark.startDistance(), spark.maxDistance());
        if (headDistance <= tailDistance + 0.03D) return;

        Vec3 start = spark.origin().add(spark.direction().scale(tailDistance));
        Vec3 end = spark.origin().add(spark.direction().scale(headDistance));
        Vec3 side = spark.direction().cross(camera.subtract(start.add(end).scale(0.5D)));
        if (side.lengthSqr() < 1.0e-6D) side = spark.direction().cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-6D) side = spark.direction().cross(new Vec3(1, 0, 0));
        if (side.lengthSqr() < 1.0e-6D) return;
        side = side.normalize();

        quad(buffer, matrix, start, end, side, camera, spark.width() * 2.1D, 1.0F, 0.38F, 0.03F, alpha * 0.34F);
        quad(buffer, matrix, start, end, side, camera, spark.width(), 1.0F, 0.92F, 0.28F, alpha);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 side, Vec3 camera,
                             double halfWidth, float red, float green, float blue, float alpha) {
        Vec3 offset = side.scale(halfWidth);
        vertex(buffer, matrix, start.subtract(offset).subtract(camera), red, green, blue, alpha);
        vertex(buffer, matrix, start.add(offset).subtract(camera), red, green, blue, alpha);
        vertex(buffer, matrix, end.add(offset.scale(0.35D)).subtract(camera), red, green, blue, 0.0F);
        vertex(buffer, matrix, end.subtract(offset.scale(0.35D)).subtract(camera), red, green, blue, 0.0F);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 pos, float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(red, green, blue, alpha);
    }

    private static Vec3 scatterDirection(Vec3 forward, RandomSource random) {
        Vec3 f = forward.normalize();
        Vec3 up = Math.abs(f.y) < 0.92D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = f.cross(up).normalize();
        Vec3 orthoUp = right.cross(f).normalize();
        double cos = Mth.lerp(random.nextDouble(), 0.35D, 1.0D);
        double sin = Math.sqrt(Math.max(0.0D, 1.0D - cos * cos));
        double phi = random.nextDouble() * Math.PI * 2.0D;
        return f.scale(cos)
            .add(right.scale(Math.cos(phi) * sin))
            .add(orthoUp.scale(Math.sin(phi) * sin))
            .normalize();
    }

    private static void trimActiveSparks() {
        int overflow = SPARKS.size() - MAX_ACTIVE_SPARKS;
        if (overflow <= 0) return;
        SPARKS.subList(0, overflow).clear();
    }

    private record Spark(Vec3 origin, Vec3 direction, double startDistance, double maxDistance, double speed,
                         float width, long startTick, int lifetimeTicks, float intensity) {}

    private ClientArmorSparkVisuals() {}
}
