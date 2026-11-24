# 0.1.3 配置菜单验收

配置页直接读写客户端的 `config/comfortable-items.json`。游戏内更改使用同一个整理键绑定，复验时用不同键位区分前后的工作状态。

| Minecraft 与实机加载器 | 游戏内入口 | 配置页验收 | MrCrayfish Configured |
| --- | --- | --- | --- |
| Forge 1.20.1 · 47.4.23 | Forge Mods → Config | 4/4 | 2.2.3 · 保存通过 |
| NeoForge 1.21.1 · 21.1.265 | NeoForge Mods → Config | 4/4 | 2.6.3 · 保存通过 |
| NeoForge 1.21.11 · 21.11.48 | NeoForge Mods → Config | 4/4 | 2.7.4 · 保存通过 |
| NeoForge 26.3 · 26.3.0.115 | NeoForge Mods → Config | 4/4 | 未见上游 26.3 发行 |
| Fabric 1.20.1 · Loader 0.19.5 | Mod Menu 7.2.2 | 4/4 | 2.2.3 · 保存通过 |
| Fabric 1.21.1 · Loader 0.19.5 | Mod Menu 11.0.5 | 4/4 | 2.6.3 · 保存通过 |
| Fabric 1.21.11 · Loader 0.18.4 | Mod Menu 17.0.1 | 4/4 | 2.7.4 · 保存通过 |
| Fabric 26.3 · Loader 0.19.5 | Mod Menu 21.0.1 | 4/4 | 未见上游 26.3 发行 |

2026-11-24 使用副屏 `DISPLAY2` 和待发布安装包逐档操作。四项检查为打开配置并取消、拒绝无效整理键、保存时只改一个字段、原版按键绑定立即显示新键并将另一键写回 JSON。Configured 的六档另验证打开其页面并只保存被修改的滚轮开关。

NeoForge 1.21.1 额外装入 Catalogue 1.11.2、Configured 2.6.3 和 Framework 0.13.11，复现截图中的菜单布局。点击舒适物品管理的 Config 后打开配置页，保存只修改所选字段。另一独立实例保存 `K` 后重启并连上原版服务端。从箱子的空槽按 `K`，服务端确认石头合并为 13 个，泥土 5 个，旧键 `R` 和 `J` 均未触发排序。

上游来源为 [Configured 发布页](https://github.com/MrCrayfish/Configured/releases)、[Configured CurseForge 文件](https://www.curseforge.com/minecraft/mc-mods/configured/files/all)及 [Mod Menu 发布页](https://github.com/TerraformersMC/ModMenu/releases)。Configured 指 MrCrayfish 的项目。安装可选菜单时，按该菜单的要求同时安装其依赖。

配置截图、按键与服务端读数见 [0.1.3 的实测证据](https://github.com/YuZhezA/comfortable-item-management/releases/download/v0.1.3/qa-evidence.zip)。
