# 上游共存记录

发布源为项目作者在 Modrinth 上传的版本。以下版本明确声明支持相应游戏版本及加载器。检测 Fabric 模组 ID `itemscroller`，(Neo)Forge 移植检测 ID `rocknroller`。`Rock'n Roller` 0.4.0 及之后的源码位于 [CagayakeGirls/itemscroller-neoforge](https://github.com/CagayakeGirls/itemscroller-neoforge)。

| 组合 | 可安装的上游版本 | 0.1.1-dev | 0.1.0-dev |
| --- | --- | --- | --- |
| Forge 1.20.1 | [Rock'n Roller 0.1.4](https://modrinth.com/mod/rocknroller/version/rJ5AZYNA) | 7/7 | 6/6 |
| NeoForge 1.21.1 | [Rock'n Roller 0.4.0](https://modrinth.com/mod/rocknroller/version/vsVKkDaQ) | 7/7 | 6/6 |
| NeoForge 1.21.11 | [Rock'n Roller 0.4.1](https://modrinth.com/mod/rocknroller/version/XdnPI2Uc) | 7/7 | 6/6 |
| NeoForge 26.3 | [Rock'n Roller 发行清单](https://api.modrinth.com/v2/project/rocknroller/version) 无 26.3 | 无上游发行 | 无上游发行 |
| Fabric 1.20.1 | [Item Scroller 0.20.0](https://modrinth.com/mod/item-scroller/version/XdGX4JFd) | 7/7 | 6/6 |
| Fabric 1.21.1 | [Item Scroller 0.24.60](https://modrinth.com/mod/item-scroller/version/BmE8Lr1a) | 7/7 | 6/6 |
| Fabric 1.21.11 | [Item Scroller 0.30.8](https://modrinth.com/mod/item-scroller/version/dCHMUnrw) | 7/7 | 6/6 |
| Fabric 26.3 | [Item Scroller 0.33.1](https://modrinth.com/mod/item-scroller/version/MvoqaGhE) | 7/7 | 6/6 |

上游默认动作以各下载构件的元数据及源码为准。已检查的 Fabric 26.3 构件整理键为 `Ctrl+R`，Fabric 与 NeoForge 1.21.1 和 1.21.11 构件整理键为 `R`，两个 1.20.1 构件没有 `SORT_INVENTORY` 字段，批量移动所有物品为 `Alt+Shift+左键`。拖动组、留一件、单个及同类分别为 `Shift+左键`、`Shift+右键`、`Ctrl+左键`、`Alt+左键`。滚动选项包含普通、Shift、Alt 及 Alt+Shift。`Shift+点击界面外` 和 `Ctrl+Shift+丢弃键` 属于上游可用的匹配丢弃输入。


0.1.1-dev 新增箱子中的共享 `Shift` + 左键拖拽验证。在真实客户端扫过相邻两格后，原版服务端确认两组物品各进入箱子一次，来源槽为空。共存时，个人背包和工作台上的共享手势同样遵循所装上游的规则。

其余用例在同版原版服务端复核普通滚轮只移动一件、共享 Alt+Shift 左键按上游规则包含快捷栏、专属 Ctrl 组合只作用于匹配物品、丢弃时保留容器物品、共享或独有的整理键以及自动补充。上游默认关闭整理时，共有的 `R` 保留给上游。将本模组的整理键改为另一按键即可单独使用。
