# 上游行为核对

## Item Scroller 与 Forge 移植

对照 [Item Scroller `9d67667`](https://github.com/sakura-ryoko/itemscroller/tree/9d67667) 和 [ItemScroller-Forge `88647aa`](https://github.com/ThinkingStudios/ItemScroller-Forge/tree/88647aa) 的 README、`Hotkeys`、`InputHandler`、`InventoryUtils`、配置和许可。各实测上游构件以[共存矩阵](coexistence.md)为准。不同版本的整理键从实际 JAR 的 `Hotkeys` 核实。

| 上游入口 | 本模组默认行为 |
| --- | --- |
| 普通、Shift、Alt、Alt+Shift 滚轮 | 单件、整组、同类、整侧，Alt 按规格区分前滚与反向取回 |
| Shift 拖动、Ctrl 拖动、Alt 拖动 | 整组、留一件、单件、同类 |
| 丢弃键拖动 | 每槽一件、整组、留一件，跟随原版丢弃键 |
| `keyMoveEverything` | 保留 Alt+Shift 左键，另有目标侧筛选和 Ctrl 快捷栏范围 |
| `enableShiftPlaceItems` | 将能完整放入的光标堆叠放入普通空槽，并向同侧拉取对侧同类 |
| `dropAllMatching`、`enableShiftDropItems` | 保留 Ctrl+Shift 丢弃与持物 Shift 点外侧，另按规格实现 Alt 组合和玩家侧范围 |
| `sortInventory`、`sortMethodDefault` | 整理键、分类或物品的名称、数量、稀有度及内部注册序号排序 |
| 收纳容器分组与优先顺序 | 保留潜影盒、收纳袋分组，类别优先和物品优先配置。堆叠兼容性交给原版槽位与物品数据判定 |
| W/S、快捷合成和村民交易 | 按范围移除 |

上游调试、配方储存和配置 GUI 属于上游工具面。本模组用 JSON 配置管理手势，原版按键绑定界面只注册整理键。上游采用 LGPLv3。本模组依据操作行为独立实现。

## Stack Refill 与 Configured

- [Stack Refill `eb22387`](https://github.com/Serilum/Stack-Refill/tree/eb22387) 的 README、源码与许可用于核对取样、背包备件和丢弃行为。许可为 All Rights Reserved。本模组通过客户端原版槽位点击实现补充。临界耐久、备件优先级、四护甲槽和副手分类由本项目规格决定。
- [Configured `9f78baf`](https://github.com/MrCrayfish/Configured/tree/9f78baf) 作为可选设置界面的参考。首发保存标准 JSON，后续接入应编辑同一路径。其模组源代码使用 LGPLv3。
