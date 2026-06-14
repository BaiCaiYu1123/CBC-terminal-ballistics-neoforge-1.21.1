package com.cbc_terminal_ballistics.network;

import com.cbc_terminal_ballistics.CBCTerminalBallistics;
import com.cbc_terminal_ballistics.ballistics.TBCaliber;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundArmorSparkPacket(BlockPos pos, Vec3 origin, Vec3 direction, long seed,
                                          float intensity, TBCaliber caliber) implements CustomPacketPayload {

    public static final Type<ClientboundArmorSparkPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CBCTerminalBallistics.MOD_ID, "armor_sparks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundArmorSparkPacket> STREAM_CODEC = StreamCodec.of(
        (StreamEncoder<RegistryFriendlyByteBuf, ClientboundArmorSparkPacket>) (buf, packet) -> packet.encode(buf),
        (StreamDecoder<RegistryFriendlyByteBuf, ClientboundArmorSparkPacket>) ClientboundArmorSparkPacket::decode
    );

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        writeVec3(buf, origin);
        writeVec3(buf, direction);
        buf.writeLong(seed);
        buf.writeFloat(intensity);
        buf.writeEnum(caliber);
    }

    private static ClientboundArmorSparkPacket decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        Vec3 origin = readVec3(buf);
        Vec3 direction = readVec3(buf);
        long seed = buf.readLong();
        float intensity = buf.readFloat();
        TBCaliber caliber = buf.readEnum(TBCaliber.class);
        return new ClientboundArmorSparkPacket(pos, origin, direction, seed, intensity, caliber);
    }

    private static void writeVec3(RegistryFriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundArmorSparkPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Class<?> handler = Class.forName("com.cbc_terminal_ballistics.client.ClientPacketHandlers");
                handler.getMethod("handleArmorSparks", ClientboundArmorSparkPacket.class).invoke(null, packet);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
