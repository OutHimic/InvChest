# InvChest

[简体中文](https://github.com/OutHimic/InvChest/blob/main/README.md)  |  [ENGLISH](https://github.com/OutHimic/InvChest/blob/main/README_EN.md)

一个Bukkit插件，让木桶里的物品自动流入玩家的口袋！

## 温馨提示

- 现在还不推荐大家使用，因为还有亿点点_{小Bug} ：
- 被转移的物品会留在箱子里，不知道怎么删掉qwq所以就会把物品无限复制给玩家……
- 我不会Java，我需要技术人员的帮助！！！！

## 功能特性

- 🔗 **木桶绑定机制**：通过命令将木桶绑定到玩家物品栏
- 🔄 **自动物品转移**：绑定木桶中的物品会自动转移到玩家物品栏
- 🌍 **全服务器范围**：支持全服务器范围的自动转移
- 🛡️ **权限系统**：完整的权限控制，保护玩家数据安全
- 🌐 **多语言支持**：支持中文和英文，可轻松扩展其他语言
- ⚙️ **高度可配置**：通过配置文件自定义各种参数

## 安装要求

- **Minecraft版本**: 1.21+ (推荐 Paper 服务端)
- **Java版本**: Java 17+
- **向下兼容**: Java 8+ (Minecraft 1.12+), Java 17+ (Minecraft 1.18+)

## 安装方法

1. 下载最新的 `InvChest.jar` 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 插件会自动生成配置文件和语言文件（中英）

## 命令使用

### 基础命令
- `/invchest` - 将目视的木桶绑定到自己的物品栏

### 高级命令  
- `/invchest [玩家ID]` - 将目视的木桶绑定到指定玩家的物品栏
- `/invchest reload` - 重载插件配置

## 权限节点

- `invchest.bind` - 基础绑定权限 (默认: true)
- `invchest.bind.others` - 绑定他人权限 (默认: op)
- `invchest.reload` - 重载插件权限 (默认: op)

## 配置说明

插件会在 `plugins/InvChest/` 目录下生成以下文件：

### config.yml
```yaml
# InvChest 配置文件
# 语言设置，支持 zh_cn, en_us 等
language: zh_cn

# 调试模式
debug: false

# 物品转移设置
transfer:
  # 自动转移间隔（秒）
  interval: 5
  # 是否启用全服务器范围自动转移
  global-transfer: true
  # 转移时是否显示提示消息（默认关闭，避免服务器刷屏）
  show-messages: false

# 木桶绑定设置
binding:
  # 最大绑定木桶数量（每个玩家）
  max-bindings: 10
  # 绑定后是否允许其他玩家打开
  allow-others-open: false
  # 绑定后是否允许破坏（默认启用，破坏后自动解绑）
  allow-break: true
```

### 语言文件
- `lang/zh_cn.yml` - 中文语言文件
- `lang/en_us.yml` - 英文语言文件

## 使用示例

1. **绑定木桶到自己**：
   - 目视一个木桶
   - 执行 `/invchest`
   - 木桶会绑定到你的物品栏

2. **绑定木桶到其他玩家**：
   - 目视一个木桶
   - 执行 `/invchest OutHimic`
   - 木桶会绑定到玩家 OutHimic 的物品栏

3. **物品自动转移**：
   - 绑定后，木桶中的物品会自动转移到玩家物品栏
   - 转移间隔可在配置中调整
   - 玩家也可以蹲下右键立即转移物品

## 开发信息

### 项目结构
```
src/main/java/cn/craftime/invChest/
├── InvChest.java              # 主插件类
├── ConfigManager.java         # 配置管理器
├── BarrelManager.java         # 木桶管理器
├── commands/
│   └── InvChestCommand.java   # 命令执行器
├── listeners/
│   ├── PlayerInteractionListener.java # 事件监听器
│   └── BarrelBreakListener.java #木桶破坏解绑监听
└── tasks/
    └── ItemTransferTask.java  # 自动转移任务
```

### 构建说明
使用 Maven 构建：
```bash
mvn clean package
```

构建后的插件文件会在 `/target/` 目录中生成。

## 技术特性

- ✅ **线程安全**：使用 ConcurrentHashMap 确保线程安全
- ✅ **原子操作**：物品转移采用原子操作，避免物品丢失（你放心好了绝对丢不了，丢一个我给你赔到物品栏满了为止）
- ✅ **内存优化**：高效的数据结构和缓存机制
- ✅ **错误处理**：完善的异常处理和日志记录
- ✅ **性能监控**：调试模式和性能监控

## 注意事项

- 木桶绑定后默认无法被其他玩家打开
- 玩家蹲下右键可以放置物品到木桶周围（如接漏斗）
- 物品转移遵循"先尝试添加，再根据结果移除"的原则，确保物品安全
- 插件重启后绑定数据会丢失（内存存储，我不知道怎么存文件）

## 支持与反馈

如有问题或建议，请访问项目主页：
https://github.com/OutHimic/InvChest

## 许可证

本项目采用无许可证，当成时 CC0 就行。
