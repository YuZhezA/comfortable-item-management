# 舒适物品管理

Minecraft Java 客户端模组。提供容器转移、鼠标滚动和拖拽、整理、批量丢弃和自动补充。使用原版容器点击和丢弃操作。Fabric 版只需要 Fabric Loader。

当前产物版本为 `0.1.0-dev`。将与你的 Minecraft 版本及加载器完全对应的一个 JAR 放进客户端 `mods` 目录。安装包、SHA-256 与证据 ZIP 在[Releases](https://github.com/YuZhezA/comfortable-item-management/releases/tag/v0.1.0-dev)。逐档状态见[支持矩阵](docs/support.md)，实际加载器版本与复验方法见[实机验收记录](docs/playtest.md)。

## 快捷键

下表是默认按键，由单键到多键组合排列。「丢弃键」跟随游戏内的绑定，原版默认为 `Q`。批量整理和转移以鼠标悬停的槽位为准。

### 单键与滚轮

| 输入 | 效果 |
| --- | --- |
| `R` | 整理悬停的箱子或背包区域。空槽也能触发，背包整理保留快捷栏 |
| 滚轮 | 在两侧之间转移一个物品 |
| `Shift` + 滚轮 | 在两侧之间转移一组物品 |
| `Alt` + 滚轮 | 以悬停物品取样。前滚把该侧所有匹配物品送往对侧，后滚从对侧取回 |
| `Alt` + `Shift` + 滚轮 | 批量转移整侧物品 |

### 单击与拖拽

| 输入 | 效果 |
| --- | --- |
| `Ctrl` + 左键拖拽 | 每经过一个槽转移一件 |
| `Shift` + 左键拖拽 | 每经过一个槽转移整组 |
| `Shift` + 右键拖拽 | 每经过一个槽转移到只剩一件 |
| `Alt` + 左键拖拽 | 按经过的槽位取样，转移该侧所有匹配物品 |
| 持物时 `Shift` + 左键点空槽 | 放下光标物品，再从对侧拉取匹配物品 |
| `Alt` + `Shift` + 左键单击 | 转移来源侧全部物品，玩家侧排除快捷栏 |
| `Alt` + `Shift` + 右键单击 | 只转移与目标侧操作前已有物品匹配的物品，玩家侧排除快捷栏 |
| 上述任一批量单击再按住 `Ctrl` | 存入和取回时，玩家侧均包含快捷栏 |

### 丢弃物品

| 输入 | 效果 |
| --- | --- |
| `Shift` + 丢弃键 | 丢弃主手或悬停槽的一整组 |
| `Alt` + 丢弃键 | 以主手或悬停物品取样，丢弃玩家侧所有匹配组，包含快捷栏 |
| `Ctrl` + `Shift` + 丢弃键 | 同样按取样丢弃玩家侧的匹配组。此组合沿用自 Item Scroller |
| 丢弃键 + 左键拖拽 | 每经过一个槽丢一件 |
| `Shift` + 丢弃键 + 左键拖拽 | 每经过一个槽丢整组 |
| `Shift` + 丢弃键 + 右键拖拽 | 每经过一个槽丢到只剩一件 |
| `Alt` + `Shift` + 丢弃键 | 丢弃背包物品。打开容器时，只丢弃与容器存量匹配的玩家物品。均排除快捷栏 |
| `Alt` + `Shift` + `Ctrl` + 丢弃键 | 同上，并包含快捷栏 |

持物时，以下组合需**单击界面有效区域外**。

| 输入 | 效果 |
| --- | --- |
| `Alt` 或 `Shift` + 单击 | 丢弃光标物品及玩家侧所有匹配组，包含快捷栏 |
| `Alt` + `Shift` + 单击 | 按同组合的丢弃键规则处理背包，排除快捷栏，保留光标物品 |
| `Alt` + `Shift` + `Ctrl` + 单击 | 同上，并包含快捷栏，保留光标物品 |

匹配比较完整 NBT 或数据组件，忽略堆叠数量。转移优先填充可合并的堆叠，再使用空槽，放不下的部分留在来源。点按式批量丢弃每次按下执行一轮。以容器存量筛选丢弃时，容器物品保留。

与 Item Scroller（`itemscroller`）或 Rock’n Roller（`rocknroller`）共存时，共有默认手势和冲突的整理键由上游处理。本模组的独有手势和自动补充仍可使用。[共存矩阵](docs/coexistence.md)

开发版的保守默认值为生存模式普通存储槽。排序和批量转移避开原版专用槽。持物执行保留光标的批量丢弃时，先借一个合适的空槽，再取回原物品。容器和背包均无合适空槽时保留物品。快捷栏在允许参与的目标侧中排在背包空槽之后。选定的备件来自背包主体，耐久相同按槽位索引由小到大。世界中的主动丢弃默认抑制补充。使用后留在手上的容器或产生的其他物品保留在原槽，补充只在消费或损坏造成空槽时触发。

## 配置

首次启动生成 `config/comfortable-items.json`。在游戏退出后编辑，重启生效。仅整理键会出现在「按键绑定」的物品栏类别。游戏修改整理键时同时保存至 JSON，启动时读取 JSON。没有启用资源加载器的部分 Fabric 实例会以 `key.comfortable_items.sort` 显示这一行。`gestures` 各字段及默认值如下。可置为 `OFF` 禁用对应操作。`DROP` 跟随游戏的丢弃键绑定。修改时保留末尾的 `LEFT`、`RIGHT`、`WHEEL`、`DROP` 或 `CLICK` 事件类型，并使用 `+` 连接修饰键。

| 字段 | 默认值与含义 |
| --- | --- |
| `enabled` | `true`，模组总开关 |
| `sortKey` | `key.keyboard.r`，原版键名 |
| `positionAwareScroll` | `true`，普通和 Shift 滚动按区域调整方向 |
| `reverseSingleScroll` / `reverseStackScroll` | `false`，反转相应滚动方向 |
| `sortEnabled` | `true`，启用整理键 |
| `sortOrder` | `CATEGORY_NAME`，另支持 `CATEGORY_COUNT`、`CATEGORY_RARITY`、`CATEGORY_RAWID`、`ITEM_NAME`、`ITEM_COUNT`、`ITEM_RARITY`、`ITEM_RAWID`。旧值 `NAME`、`ID`、`COUNT` 仍有效 |
| `sortCategoryOrder` | `[]`，可给 `block`、`food`、`item`、`equipment/pickaxe` 等分类指定先后，未列出的分类随后按名称排列 |
| `sortShulkersAtEnd` / `sortBundlesAtEnd` | `true`，潜影盒和收纳袋放在末尾 |
| `sortShulkersInverted` / `sortBundlesInverted` | `false`，将对应末尾分组改到前面 |
| `sortTopPriority` / `sortBottomPriority` | `[]`，接受按顺序排列的物品 ID |
| `refill` / `suppressRefillOnDrop` | `true`，启用补充并抑制主动丢弃后的补充 |
| `refillItems` | `ID`，另有 `OFF`、`STRICT` |
| `refillEquipment` / `refillTools` | `ID`，另有 `OFF`、`TYPE`、`STRICT` |
| `replaceEquipment` / `replaceTools` | `OFF`，也可以设为 `TYPE`、`ID`、`STRICT` |

| `gestures` 字段 | 默认值 |
| --- | --- |
| `moveAll` / `moveMatching` | `ALT+SHIFT+LEFT` / `ALT+SHIFT+RIGHT` |
| `moveAllWithHotbar` / `moveMatchingWithHotbar` | `ALT+SHIFT+CTRL+LEFT` / `ALT+SHIFT+CTRL+RIGHT` |
| `scrollOne` / `scrollStack` / `scrollMatching` / `scrollAll` | `WHEEL` / `SHIFT+WHEEL` / `ALT+WHEEL` / `ALT+SHIFT+WHEEL` |
| `dragStacks` / `dragLeaveOne` / `dragOne` / `dragMatching` | `SHIFT+LEFT` / `SHIFT+RIGHT` / `CTRL+LEFT` / `ALT+LEFT` |
| `placeMatching` | `SHIFT+LEFT`，在持物点空槽时生效 |
| `dragDropOne` / `dragDropStacks` / `dragDropLeaveOne` | `DROP+LEFT` / `SHIFT+DROP+LEFT` / `SHIFT+DROP+RIGHT` |
| `dropStack` / `dropMatching` | `SHIFT+DROP` / `ALT+DROP` |
| `dropAll` / `dropAllWithHotbar` / `upstreamDropMatching` | `ALT+SHIFT+DROP` / `ALT+SHIFT+CTRL+DROP` / `CTRL+SHIFT+DROP` |
| `outsideMatchingAlt` / `outsideMatchingShift` | `ALT+CLICK` / `SHIFT+CLICK` |
| `outsideAll` / `outsideAllWithHotbar` | `ALT+SHIFT+CLICK` / `ALT+SHIFT+CTRL+CLICK` |

启用损坏前替换时，原件低于 10 点且有至少剩余 10 点的备件才互换。装备和工具严格补充比较除当前损耗以外的数据。护甲按穿戴槽筛选，盾牌作装备，火把和图腾作普通物品。可选的 Configured 设置界面尚未接入。JSON 可独立使用。

## 构建与测试

- **Fabric 1.20.1** 使用 JDK 17，从仓库根运行 `./gradlew build`。
- **Forge 1.20.1、Fabric 1.21.1、NeoForge 1.21.1、NeoForge 1.21.11** 用对应版本的 JDK，运行 `./gradlew -p targets/<加载器>-<Minecraft 版本> build`。1.20.1 用 JDK 17，1.21.x 用 JDK 21。
- **Fabric 1.21.11、Fabric 26.3、NeoForge 26.3** 用 Gradle 9.7.0，运行 `gradle -p targets/<加载器>-<Minecraft 版本> build`。1.21.11 用 JDK 21，26.3 用 JDK 25。
- 产物在各项目的 `build/libs/`。设置环境变量 `COMFORT_ITEMS_BUILD_DIR` 可将构建输出写至其他目录，避免缓存和产物占用工作区磁盘。
- **2026-11-24** 八个重点组合逐个构建。在副屏上的干净客户端连接同版 Mojang 原版专用服务端，每档通过 31 项核心断言和 22 项扩展断言。七个可安装上游的组合各通过 6 项共存断言。实际加载器版本、场景和证据见[实机验收记录](docs/playtest.md)及[共存矩阵](docs/coexistence.md)。

## 制作与协作

- [YuZhezA](https://github.com/YuZhezA) 负责需求设计和项目维护。
- [OpenAI Codex（GPT）](https://github.com/codex) 参与模组实现、实机测试及文档整理。

## 参考与许可

[上游行为核对表](docs/references.md)记录对应源码、快捷键与取舍。基于行为和文档独立实现。Item Scroller 的权限文件为 LGPLv3，Forge 移植仓库同样为 LGPLv3。Stack Refill 源仓库标为 All Rights Reserved，仅参考公开行为。Configured 提供 LGPLv3 模组许可，目前只作为可选集成的原始资料。

- [Item Scroller](https://github.com/sakura-ryoko/itemscroller/tree/9d67667)
- [ItemScroller-Forge / Rock'n Roller](https://github.com/ThinkingStudios/ItemScroller-Forge/tree/88647aa)
- [Stack Refill](https://github.com/Serilum/Stack-Refill/tree/eb22387)
- [Configured](https://github.com/MrCrayfish/Configured/tree/9f78baf)

## 首发改动

- 实现容器与背包批量转移、拖动、丢弃和整理。持物 Shift 放置只向能完整容纳光标堆叠的普通空槽执行。排序和合并遵循原版堆叠限制，保留潜影盒和收纳袋数据。
- 接入原版整理键绑定和 JSON 持久化。与上游共存时保留其默认手势与实际整理键。
- 新增普通物品、工具和装备补充，以及可选的低耐久互换。扩展轮次覆盖严格数据匹配、四护甲槽及耐久边界。

图腾按普通物品补充设置处理。收到本地玩家的原版图腾保护事件后，按主手、副手的顺序识别实际消耗的图腾。八档已通过副手保护补充及双持时主手优先的原版服务端场景。

- 重排快捷键说明，按基础操作、鼠标组合和丢弃分组，并把八个实测安装包列在发行页。
