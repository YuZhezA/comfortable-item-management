# 舒适物品管理

Minecraft Java 客户端模组。提供容器转移、鼠标滚动和拖拽、整理、批量丢弃和自动补充。使用原版容器点击和丢弃操作。Fabric 版只需要 Fabric Loader。

当前为开发构建 `0.1.0-dev`。各加载器和游戏版本的状态见[逐档支持矩阵](docs/support.md)。正式验收包括安装产物启动、原版服务端同步及可用时的 Item Scroller 共存。

## 快捷键

| 快捷键 | 行为 |
| --- | --- |
| Alt+Shift+左键 / 右键 | 整侧转移 / 按目标侧现有物品严格筛选 |
| Alt+Shift+Ctrl+左键 / 右键 | 同上，并包含玩家快捷栏 |
| Alt+滚轮 | 悬停槽取样，前滚向对侧转移全部同类，后滚取回 |
| 普通 / Shift+滚轮 | 转移一个物品 / 转移一组 |
| Alt+Shift+滚轮 | 批量转移整侧物品 |
| Shift+左键拖拽 / Shift+右键拖拽 | 移动整组 / 留一件 |
| Ctrl+左键拖拽 / Alt+左键拖拽 | 移动一件 / 转移同类 |
| 丢弃键+左键拖拽 | 每个经过的槽丢一件 |
| Shift+丢弃键+左键 / 右键拖拽 | 每槽丢整组 / 每槽留一件 |
| R | 整理悬停区域，包括空槽。背包整理保留快捷栏 |
| Shift+丢弃键 | 丢弃手持或悬停的整组 |
| Alt+丢弃键 | 以手持或悬停物品取样，丢弃玩家侧所有匹配组，含快捷栏 |
| Alt+Shift+丢弃键 | 丢弃背包物品。打开容器时按容器存量筛选 |
| Alt+Shift+Ctrl+丢弃键 | 同上，并包含快捷栏 |
| 持物 Alt 或 Shift 单击界面外 | 丢弃光标物品及玩家侧匹配组 |
| 持物 Alt+Shift 单击界面外，可加 Ctrl | 执行对应背包范围的丢弃，保留光标物品 |

批量操作按悬停区域决定方向。优先填充可合并的堆叠，再使用空槽，放不下的部分留在来源。严格匹配比较整个 NBT 或数据组件并忽略数量。点按式批量丢弃只处理按下事件。`Ctrl+Shift+丢弃键` 保留为上游匹配丢弃手势。与 `itemscroller` 或 `rocknroller` 共存时，共有的默认手势交给上游。整理键发生冲突时交给上游。已检查的 1.21.1 和 1.21.11 上游构件声明了 `R`，Fabric 26.3 上游构件声明了 `Ctrl+R`，已检查的 1.20.1 上游构件没有整理绑定。自动补充继续启用。共存功能实测按支持矩阵记录。

开发版的保守默认值为生存模式普通存储槽。排序和批量转移避开原版专用槽。持物执行保留光标的批量丢弃时，先借一个合适的空槽，再取回原物品。容器和背包均无合适空槽时保留物品。快捷栏在允许参与的目标侧中排在背包空槽之后。选定的备件来自背包主体，耐久相同按槽位索引由小到大。世界中的主动丢弃默认抑制补充。

## 配置

首次启动生成 `config/comfortable-items.json`。在游戏退出后编辑，重启生效。仅整理键会出现在「按键绑定」的物品栏类别。游戏修改整理键时同时保存至 JSON，启动时读取 JSON。`gestures` 各字段默认值见[快捷键表](#快捷键)及生成文件。字段值以 `+` 分隔，可置为 `OFF` 禁用对应操作。`DROP` 跟随游戏的丢弃键绑定。

| 字段 | 默认值与含义 |
| --- | --- |
| `enabled` | `true`，模组总开关 |
| `sortKey` | `key.keyboard.r`，原版键名 |
| `positionAwareScroll` | `true`，普通和 Shift 滚动按区域调整方向 |
| `reverseSingleScroll` / `reverseStackScroll` | `false`，反转相应滚动方向 |
| `sortOrder` | `CATEGORY_NAME`，另支持 `NAME`、`ID`、`COUNT` |
| `sortShulkersAtEnd` / `sortBundlesAtEnd` | `true`，潜影盒和收纳袋放在末尾 |
| `sortShulkersInverted` / `sortBundlesInverted` | `false`，将对应末尾分组改到前面 |
| `sortTopPriority` / `sortBottomPriority` | `[]`，接受按顺序排列的物品 ID |
| `refill` / `suppressRefillOnDrop` | `true`，启用补充并抑制主动丢弃后的补充 |
| `refillItems` | `ID`，另有 `OFF`、`STRICT` |
| `refillEquipment` / `refillTools` | `ID`，另有 `OFF`、`TYPE`、`STRICT` |
| `replaceEquipment` / `replaceTools` | `OFF`，也可以设为 `TYPE`、`ID`、`STRICT` |

启用损坏前替换时，原件低于 10 点且有至少剩余 10 点的备件才互换。装备和工具严格补充比较除当前损耗以外的数据。护甲按穿戴槽筛选，盾牌作装备，火把和图腾作普通物品。Configured 接入待验证。JSON 可独立使用。

## 构建与测试

- **Fabric 1.20.1** 使用 JDK 17，执行 `./gradlew build`，产物位于 `build/libs/`。
- **其他重点档** 各自使用 `targets/<加载器>-<版本>`。构建脚本固定加载器版本，Java 1.21.x 档使用 JDK 21，26.3 档使用 JDK 25。验证状态见矩阵。
- **2026-11-24 开发进度** 已在副屏运行 Fabric 1.20.1 / Loader 0.16.14 / Java 17 的干净开发客户端，并连接 Minecraft Java 1.20.1 本机原版专用服务端。首轮实测覆盖两向转移、匹配筛选、快捷栏范围、Alt 滚轮两向、满箱、空槽整理、容器和世界丢弃、光标保留和主手方块补充。此轮证据对应开发客户端。安装产物、完整补充边界、改键和共存仍待逐项记录。

## 参考与许可

基于行为和文档独立实现。Item Scroller 的权限文件为 LGPLv3，Forge 移植仓库同样为 LGPLv3。Stack Refill 源仓库标为 All Rights Reserved，仅参考公开行为。Configured 提供 LGPLv3 模组许可，目前只作为可选集成的原始资料。

- [Item Scroller](https://github.com/sakura-ryoko/itemscroller/tree/9d67667)
- [ItemScroller-Forge / Rock'n Roller](https://github.com/ThinkingStudios/ItemScroller-Forge/tree/88647aa)
- [Stack Refill](https://github.com/Serilum/Stack-Refill/tree/eb22387)
- [Configured](https://github.com/MrCrayfish/Configured/tree/9f78baf)

开发过程中的安装 JAR 验证使用 `tools/launch-fixture.py` 和 `tools/play-fixture.py`。游戏服务端使用 Mojang 原版 JAR，测试数据和凭据分别保存在隔离实例中。首次并行构建受工作区盘空间影响，设置 COMFORT_ITEMS_BUILD_DIR 可选择其他生成目录。

本次改动处理了上游整理绑定冲突、离线测试身份、NeoForge 启动类路径重复。测试工具使用独立实例与本地原版服务端。

Forge 1.20.1 加入 pack.mcmeta，修复安装 JAR 启动时的资源包元数据警告。测试脚本增加多人服耐久与共存检查。
