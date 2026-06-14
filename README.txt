CBC：终极弹道（NeoForge 1.21.1 移植版）
========================================

CBC：终极弹道是一个"创造：大炮"插件，可以改变方块被 CBC 弹丸击中时的反应。

本仓库是原项目（MegiTicky/CBC-terminal-ballistics）的 NeoForge 1.21.1 移植版本，
新增了 Valkyrien Skies（Sable）物理结构兼容和 Copycats+ 框架装甲方块支持。


主要特色
--------

精细的穿透力学：
  当方块被弹丸击中时，弹丸有时会穿透而非破坏方块。新增了一种材料属性——延展性。
  它决定了连续击打多少次才能打破一个方块。例如，钢制装甲具有高延展性，能够承受
  多次攻击而不破裂。

口径很重要：
  重型装甲板现在可以吸收数百发快速射击的自动炮弹而不失效，这需要你从侧翼包抄薄弱、
  无保护的区域，或者使用能一次性穿透装甲板的强力加农炮。更高口径的加农炮，如大炮，
  能对装甲方块造成显著的完整性伤害，并在两击内将其击碎。

弹丸撞击痕迹：
  自动炮、小/中小炮（CBCMS）、中炮（CBCMW）和大炮都会产生不同的穿透孔、
  阻碍凹痕和弹跳痕迹。撞击痕迹在金属和通用材质方块上有不同的视觉效果。

致命剥落：
  当穿甲弹穿透载具时，会在载具内部形成致命的金属碎片锥状飞溅，破坏玩家并破坏内部部件。

装甲模仿者：
  新增了模仿装甲层和可折叠复制装甲方块。你可以用护甲升级器物品自由调整他们的耐久度。

Valkyrien Skies / Sable 兼容（移植版新增）：
  弹丸撞击痕迹现在可以在 Sable 物理结构（舰船）上正确显示，并跟随舰船移动和旋转。
  命中金属装甲时产生火花粒子效果。剥落锥也能在物理结构内部正确生成。


依赖关系（NeoForge 1.21.1）
----------------------------

必需：
  - Create：6.0.6+
  - Copycats+：3.0.4+
  - Create Big Cannons：5.10.2+

可选：
  - Valkyrien Skies（Sable）：1.2.2+
    （未安装时所有功能正常运行，安装后弹痕可跟随物理结构移动）
  - CBC More Shells：可选
  - CBC Modern Warfare：可选


与原版的区别
------------

本移植版基于 MegiTicky 的原版 CBC Terminal Ballistics，主要改动：

  - 从 Forge 移植到 NeoForge 1.21.1
  - 新增 Sable（Valkyrien Skies）物理结构弹痕兼容
  - 新增可折叠复制装甲方块（Framed Collapsible Copycat Armor Block）
  - 新增剥落锥可视化效果
  - 新增装甲命中火花粒子效果
  - 完善 Copycats+ 集成


制作人员
--------

原作者：MegiTicky
  https://github.com/MegiTicky/CBC-terminal-ballistics

移植与新增功能：Erika

CBC：终极弹道是作为"创造：大炮"的附加组件构建的。
部分游戏逻辑基于 Create Big Cannons 源代码开发。
部分纹理基于、受其启发或改编自最初为 Create 和 Create Big Cannons 制作的素材。
Copycat Armor Layer 的实现和概念部分灵感来自 Create 和 Copycats+。

这些资产和系统的全部功劳归其各自作者所有。

非常感谢 Create，Create Big Cannons 和 Copycats+ 开发者，创建了使这个插件成为可能的项目。


许可证
------

CC BY-NC-SA 4.0（署名-非商业性使用-相同方式共享 4.0 国际）

  - 署名：必须标注原作者 MegiTicky
  - 非商业：禁止用于商业目的
  - 相同方式共享：修改版本必须使用相同许可证

完整许可证文本见 LICENSE.txt