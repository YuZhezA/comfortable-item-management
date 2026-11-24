# 实机验收记录

2026-11-24，Windows，副屏 `DISPLAY2`。测试窗口客户区为 `(-1792, 116, 984, 751)`。每次建立干净客户端实例，从 `mods` 目录加载待交付 JAR。核心测试的 `mods` 清单每次只有本模组。四个原版专用服务端分别使用对应版本的 Mojang 服务端 JAR，在本机回环网络接受测试连接。

| 加载器与游戏版本 | 构建加载器 | 实机加载器 | JDK | 核心测试 | 扩展测试 |
| --- | --- | --- | --- | --- | --- |
| Forge 1.20.1 | 47.4.29 | 47.4.23 | 17 | 31/31 | 22/22 |
| NeoForge 1.21.1 | 21.1.264 | 21.1.265 | 21 | 31/31 | 22/22 |
| NeoForge 1.21.11 | 21.11.48 | 21.11.48 | 21 | 31/31 | 22/22 |
| NeoForge 26.3 | 26.3.0.117 | 26.3.0.115 | 25 | 31/31 | 22/22 |
| Fabric 1.20.1 | 0.16.14 | 0.19.5 | 17 | 31/31 | 22/22 |
| Fabric 1.21.1 | 0.16.14 | 0.19.5 | 21 | 31/31 | 22/22 |
| Fabric 1.21.11 | 0.18.4 | 0.18.4 | 21 | 31/31 | 22/22 |
| Fabric 26.3 | 0.19.5 | 0.19.5 | 25 | 31/31 | 22/22 |

## 覆盖与证据

- 核心流程通过副屏真实键鼠输入操作箱子、背包和世界。覆盖双向转移、快栏范围、满容器、空槽整理、持物 Shift 放置、批量丢弃、主副手图腾与火把及工具消耗。服务端在每步读取库存、箱子内容和适用的副手字段。截图记录容器打开、操作后界面及世界最终画面。
- 扩展流程使用独立的 JSON 启用严格预损替换，覆盖耐久 9/10 点、备用件排序及完整物品数据比较。通过原版按键绑定界面测试改键后，直接读取 JSON，再在原版服务端用新键触发整理。
- [共存矩阵](coexistence.md)记录上游构件来源与实测结果。只有共存实例包含上游模组和其依赖。NeoForge 1.21.11 的一次共存场景被流浪商人挡住箱子，截图定位后在夹具中清理场地，再以同一个 JAR 重跑。
- 每个安装产物与测试实例的 SHA-256 逐一核对。`launch.json` 记录运行配置、实际加载器版本与装入的 JAR 哈希。`mod-config.json` 记录干净实例的默认配置与扩展轮次改键结果。`playtest.json` 记录输入、预期及原版认可的状态。`manifest.json` 汇总构建、客户端、用例和状态，并记录构建源码提交及发布文档提交。

安装 JAR 来自源码提交 `bdaf05c`。各包的 SHA-256 及截图、日志和逐项原版服务端读取详见[私有发行页](https://github.com/YuZhezA/comfortable-item-management/releases/tag/v0.1.0-dev)中的 `SHA256SUMS.txt` 和 `qa-evidence.zip`。

## 原版服务端 JAR SHA-1

| Minecraft | SHA-1 |
| --- | --- |
| 1.20.1 | `84194a2f286ef7c14ed7ce0090dba59902951553` |
| 1.21.1 | `59353fb40c36d304f2035d51e7d6e6baa98dc05c` |
| 1.21.11 | `64bb6d763bed0a9f1d632ec347938594144943ed` |
| 26.3 | `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c` |

## 复验环境

- 使用 `tools/launch-fixture.py <组合> <安装 JAR> <新实例路径> --mc-dir <启动器缓存>`。脚本需要对应加载器的启动配置、原版资源及同主版本 JDK。`PROFILES` 记录本次使用的启动器配置名。Windows 副屏坐标由 `launch-fixture.py` 与 `fixture-input.py` 共同校验，移动到其他电脑后要按实际屏幕定位。
- 同版原版服务端监听回环地址。准备名为 `ComfortQA` 的生存测试玩家，主持测试者在独立的本地服务端开启 RCON，把 `COMFORT_QA_SERVER` 设为对应服务端目录，`COMFORT_QA_INSTANCE` 设为待测客户端目录。端口见 `tools/launch-fixture.py`。RCON 用于准备夹具和读回结果。
- 执行 `python tools/play-fixture.py`。扩展轮次启动时传入 `--boot-config '{"replaceTools":"STRICT","replaceEquipment":"STRICT","sortKey":"key.keyboard.j"}'`，再执行 `python tools/play-fixture.py --extended`。共存轮次重新启动干净实例，每个上游安装包通过一个 `--with-mod` 参数加入，运行 `python tools/play-fixture.py --coexist`。需要 Python、Pillow，扩展轮次的按钮定位另需 NumPy 和 OpenCV。每轮串行输入，退出该客户端以后再启动下一轮。
