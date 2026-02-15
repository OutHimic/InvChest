# InvChest

> 本项目中包含AI生成内容，请不要将其二次投入AI训练！

一个为 Bukkit/Spigot/Paper/Purpur/Folia 服务器设计的插件，允许玩家绑定容器并将其中物品按策略自动传输到玩家背包。支持多语言与可配置的传输策略，并为后续拓展 Sponge、(Neo)Forge、Fabric 预留架构。

## Overview (English)
InvChest is a Minecraft server plugin for Bukkit-family cores that lets players bind a container (chest, trapped chest, barrel) and automatically transfer items into their inventory at an interval. It includes language localization, permissions, configurable strategies, and initial Folia-friendly scheduling approach. Architecture is prepared to extend to Sponge and modded cores later.

## 安装
- 将打包生成的 `InvChest.jar` 放入 `plugins/` 目录
- 首次启动后会生成 `config.yml` 与 `lang/` 目录（内置 `en_us.yml` 与 `zh_cn.yml`）
- 如需修改语言，在 `config.yml` 中设置 `lang`

## 构建
- JDK：Azul JDK 8（最低 Java 8）
- 构建工具：Maven
- 已配置 Xget 镜像加速依赖下载
- 执行 `mvn -DskipTests package` 生成插件

## 兼容性
- 支持核心：Bukkit、Spigot、Paper、Folia（支持）、Purpur
- 目标版本：Minecraft 1.12+
- 说明：本版本优先实现 Bukkit/Paper 系，Sponge 将在后续以独立适配层接入，同一 `.jar` 兼容将尽量保证

## 命令
- `/invchest bind` 或 `/ic bind`：绑定玩家当前所看的容器
- `/invchest unbind` 或 `/ic unbind`：解绑
- 管理员：
  - `/invchest bind <玩家名>` 或 `/ic bind <玩家名>`：为指定玩家绑定（管理员需面向目标容器）
  - `/invchest unbind <玩家名>` 或 `/ic unbind <玩家名>`：为指定玩家解绑

## 权限
- `invchest.bind`：允许玩家绑定/解绑（默认开启）
- `invchest.admin`：允许管理员为他人绑定/解绑、破坏受保护容器（仅 OP）

## 配置（`config.yml`）
- `transfer-interval`：传输间隔（秒），默认 `30`
- `transfer-mode`：`all` 或 `try`
  - `all`：尝试将容器所有物品移入玩家背包；背包不够则在玩家位置掉落；完成后清空容器
  - `try`：按顺序尽量移入；遇到无法容纳的物品时停止；容器保留剩余物品
- `max-items-per-transfer`：单次传输的物品数量上限（按堆叠数量累加）
- `redstone-output-enabled`：传输时输出红石信号（预留开关）
- `state-update-enabled`：传输时更新容器状态（预留开关）
- `lang`：如 `zh_cn`、`en_us` 或 `auto_en_us`（控制台固定指定语言，玩家根据客户端语言自动匹配）
- `debug-mode`：调试日志开关

## 容器与保护
- 支持容器：箱子、双箱、陷阱箱、双陷阱箱、桶（不支持铜质容器）
- 绑定后：
  - 任何玩家打开该容器将被阻止
  - 破坏受保护容器仅允许绑定者或具有 `invchest.admin` 的玩家
  - 允许外部输入（如漏斗、投掷器），禁止外部输出（如漏斗、漏斗矿车）
  - 容器被破坏或不存在时自动解绑

## 多语言
- 资源目录：`src/main/resources/lang/`
- 运行时目录：`plugins/InvChest/lang/`
- 默认提供：`zh_cn.yml`、`en_us.yml`
- 新增语言：将 `<code>.yml` 放入资源目录后构建即可
- 自动模式：`lang: auto_<code>` 时，控制台按 `<code>` 输出，玩家依据客户端语言匹配，不存在则回退到 `<code>`

## 架构与性能
- 模块化：核心逻辑、命令、监听、配置、存储分包
- 事件：保护、解绑、传输可通过内部事件管线扩展（后续开放）
- 性能：
  - 传输调度尽量保持主线程安全
  - 分页处理与上限控制，避免一次性搬运过大
  - 容器位置缓存，减少重复计算
- 指标：单次传输尽量控制在 50ms 内；基础运行内存小于 50MB；可同时处理 100+ 容器绑定

## 安全
- 验证容器所有权、阻止外部提取，降低复制风险
- 权限与命令参数严格校验

## API（预留）
- 后续将提供事件与接口（绑定、解绑、传输）供其他插件集成

## 变更日志
- `0.1.0` 初始版本：绑定/解绑、传输策略、容器保护、多语言与配置

## License
- MIT

## 致谢 Thanks
- Bukkit/Spigot/Paper 社区
- Purpur/Folia 项目
- Trae 提供的几乎*完美*的IDEA
- CloseAI的GPT-5-high
- pama1234的经济支持
