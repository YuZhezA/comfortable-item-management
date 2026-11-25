# 舒适物品管理

Minecraft Java 客户端模组。提供容器转移、鼠标滚动和拖拽、整理、批量丢弃和自动补充。使用原版容器点击和丢弃操作。Fabric 版只需要 Fabric Loader。

安装包请按加载器和游戏版本选择。将与你的 Minecraft 版本及加载器完全对应的一个 JAR 放进客户端 `mods` 目录。安装包、SHA-256 与证据 ZIP 在[Releases](https://github.com/YuZhezA/comfortable-item-management/releases)。逐档状态见[支持矩阵](docs/support.md)，实际加载器版本与复验方法见[实机验收记录](docs/playtest.md)，版本改动见[更新日志](CHANGELOG.md)。

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
| `Shift` + 左键单击 | 工作台优先放入 3×3 合成格。生存物品栏优先把可装备物放进对应空槽 |
| `Shift` + 左键拖拽 | 每次进入有物品的槽位，向对侧转移整组。按住不松，可经过物品落入的槽位把它移回。个人背包、工作台和创造物品栏个人背包页的两侧是背包主体与快捷栏 |
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

箱子类容器、漏斗、发射器类容器、潜影盒和带储物格的坐骑支持与背包交换。模组的拖拽、批量转移和整理跳过合成、交易、加工、装备和输出槽。工作台和生存物品栏的 `Shift` + 左键在原槽松开时执行原版快速移动。工作台合成格无法接收时，原版会尝试在背包和快捷栏之间转移。装备槽已占用时保留原装备。按住左键离开初始槽后开始连续拖拽。创造模式打开普通容器时可以使用容器手势。创造物品栏只在个人背包页接管转移拖拽，物品目录和垃圾槽沿用原版操作。持物执行保留光标的批量丢弃时，先借一个合适的空槽，再取回原物品。容器和背包均无合适空槽时保留物品。快捷栏在允许参与的目标侧中排在背包空槽之后。选定的备件来自背包主体，耐久相同按槽位索引由小到大。世界中的主动丢弃默认抑制补充。使用后留在手上的容器或产生的其他物品保留在原槽，补充只在消费或损坏造成空槽时触发。

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

启用损坏前替换时，原件低于 10 点且有至少剩余 10 点的备件才互换。装备和工具严格补充比较除当前损耗以外的数据。护甲按穿戴槽筛选，盾牌作装备，火把和图腾作普通物品。

从 `0.1.3` 起，可在游戏内编辑同一个 `config/comfortable-items.json`。点击「保存」后立即生效，取消或按 `Esc` 会舍弃未保存的编辑。配置页和原版「按键绑定」共享整理键。手工改 JSON 后重启客户端。游戏内有常规、整理、自动补充和快捷手势四组。排序优先级和类别列表使用 JSON 字符串数组，例如 `["minecraft:stone"]`。

| 加载器 | 打开配置 |
| --- | --- |
| Forge 1.20.1 | 主菜单 → Mods → 舒适物品管理 → Config |
| NeoForge 1.21.1、1.21.11、26.3 | Mods → 舒适物品管理 → Config |
| Fabric 四档 | 安装可选的 Mod Menu，通过 Mods 中的模组配置按钮进入。Fabric Loader 自身没有 Mods 配置列表 |

MrCrayfish 的 Configured 可发现本模组的客户端配置。默认打开本模组的页面。NeoForge 1.21.1 的 Catalogue 列表也可以用同一个 Config 按钮。若想使用 Configured 自己的界面，可开启 Configured 的 `forceConfiguredMenu` 后重启，再通过相同的 Config 按钮进入。Minecraft 1.20.1 使用 `config/configured-client.toml` 中的 `forceConfiguredMenu = true`。1.21.1 和 1.21.11 使用 `config/configured-client.properties` 中的 `forceConfiguredMenu=true`。Fabric 安装 Mod Menu 和对应的 Configured 后也可这样切换。已接入的 Configured 档位见[配置菜单验收记录](docs/config-menus.md)。MrCrayfish 的 Configured 暂无 26.3 发行，这一档可用上表的配置入口。Configured、Framework、Mod Menu 和 Fabric API 均为可选组件。

## 构建与测试

- **Fabric 1.20.1** 使用 JDK 17，从仓库根运行 `./gradlew build`。
- **Forge 1.20.1、Fabric 1.21.1、NeoForge 1.21.1、NeoForge 1.21.11** 用对应版本的 JDK，运行 `./gradlew -p targets/<加载器>-<Minecraft 版本> build`。1.20.1 用 JDK 17，1.21.x 用 JDK 21。
- **Fabric 1.21.11、Fabric 26.3、NeoForge 26.3** 用 Gradle 9.7.0，运行 `gradle -p targets/<加载器>-<Minecraft 版本> build`。1.21.11 用 JDK 21，26.3 用 JDK 25。
- 产物在各项目的 `build/libs/`。设置环境变量 `COMFORT_ITEMS_BUILD_DIR` 可将构建输出写至其他目录，避免缓存和产物占用工作区磁盘。
- **0.1.4（2026-11-26）** 八个重点组合在副屏使用真实客户端连接同版原版专用服务端，每档通过 32 项单击与拖拽、31 项核心和 22 项扩展检查。七档各通过 7 项上游共存检查。见[安装包和证据](https://github.com/YuZhezA/comfortable-item-management/releases/tag/v0.1.4)及[本轮验收](docs/shift-click.md)。
- **0.1.3（2026-11-24）** 八档各通过 4 项配置、31 项原版联机核心、24 项拖拽和 22 项扩展检查。六档通过 Configured 编辑验证，七档通过上游共存检查。NeoForge 1.21.1 另复验了 Catalogue 中的 Config 按钮和重启后使用新整理键。见[安装包和证据](https://github.com/YuZhezA/comfortable-item-management/releases/tag/v0.1.3)及[配置验收矩阵](docs/config-menus.md)。
- **0.1.2（2026-11-24）** 八个重点组合逐个构建，并在副屏用仅装本模组的真实客户端连接同版原版专用服务端。每档通过 24 项拖拽回归、31 项核心检查和 22 项扩展检查。七档各通过 7 项上游共存检查。加载器、用例和证据见[实机验收记录](docs/playtest.md)与[共存矩阵](docs/coexistence.md)。
- **0.1.1（2026-11-24）** 八个重点组合逐个构建。每档在副屏使用仅装本模组的 Minecraft 客户端连接同版原版专用服务端，通过 12 项拖拽回归、31 项核心检查和 22 项扩展检查。七个有上游发行的组合各通过 7 项共存检查。实际加载器版本、测试场景和证据见[实机验收记录](docs/playtest.md)与[共存矩阵](docs/coexistence.md)。

## 制作与协作

- [YuZhezA](https://github.com/YuZhezA) 负责需求设计和项目维护。
- [OpenAI Codex（GPT）](https://github.com/codex) 参与模组实现、实机测试及文档整理。

## 参考与许可

[上游行为核对表](docs/references.md)记录对应源码、快捷键与取舍。基于行为和文档独立实现。Item Scroller 的权限文件为 LGPLv3，Forge 移植仓库同样为 LGPLv3。Stack Refill 源仓库标为 All Rights Reserved，仅参考公开行为。Configured 的 API 按版本作为可选编译依赖，发布 JAR 不包含 Configured 或 Mod Menu 的实现。

- [Item Scroller](https://github.com/sakura-ryoko/itemscroller/tree/9d67667)
- [ItemScroller-Forge / Rock'n Roller](https://github.com/ThinkingStudios/ItemScroller-Forge/tree/88647aa)
- [Stack Refill](https://github.com/Serilum/Stack-Refill/tree/eb22387)
- [Configured](https://github.com/MrCrayfish/Configured/tree/9f78baf)

## 0.1.4 改动

- 工作台 `Shift` + 左键单击恢复合成格优先，支持从快捷栏和背包放入物品。
- 生存物品栏 `Shift` + 左键单击恢复装备功能。空装备槽优先，已装备的物品保持原位。
- 这两个界面在原槽松开时执行单击，按住离开原槽时可继续拖拽和往返转移。

## 0.1.3 改动

- 修复 Forge 和 NeoForge 模组列表中「舒适物品管理」的 Config 按钮不可用的问题。
- Fabric 四档接入可选的 Mod Menu。支持的 Configured 版本可编辑同一份 JSON。
- 配置页支持即时保存、取消和整理键同步。见[配置菜单验收记录](docs/config-menus.md)。

## 0.1.2 改动

- 修复持续按住 `Shift` 和左键时，刚转移的整组物品无法再移回的问题。每次进入槽位时按该槽所属的一侧确定方向，同一槽内移动鼠标只触发一次。

## 0.1.1 改动

- 统一 GitHub Release 标签、标题和安装包文件名，补齐历史更新日志。安装包沿用已完成实机验收的文件。
- 修复 `Shift` + 左键等转移拖拽在创造模式的工作台、箱子和创造物品栏个人背包页不能使用的问题。
- 独立背包与工作台的转移拖拽以按下时的位置确定方向，在背包主体和快捷栏间移动。合成格不参与，目标无空间时物品留在原槽。
- 潜影盒储物槽可参与转移，继续遵循原版的潜影盒嵌套限制。

## 首发改动

- 实现容器与背包批量转移、拖动、丢弃和整理。持物 Shift 放置只向能完整容纳光标堆叠的普通空槽执行。排序和合并遵循原版堆叠限制，保留潜影盒和收纳袋数据。
- 接入原版整理键绑定和 JSON 持久化。与上游共存时保留其默认手势与实际整理键。
- 新增普通物品、工具和装备补充，以及可选的低耐久互换。扩展轮次覆盖严格数据匹配、四护甲槽及耐久边界。

图腾按普通物品补充设置处理。收到本地玩家的原版图腾保护事件后，按主手、副手的顺序识别实际消耗的图腾。八档已通过副手保护补充及双持时主手优先的原版服务端场景。

- 重排快捷键说明，按基础操作、鼠标组合和丢弃分组，并把八个实测安装包列在发行页。
