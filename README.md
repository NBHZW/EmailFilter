EmailFilter 是一个基于 Kotlin 开发的邮箱过滤工具，专注于从 QQ 邮箱中筛选出与面试、笔试相关的重要邮件，过滤垃圾信息，帮助用户高效管理求职相关的邮件通知。该项目需搭配外部 AI 服务（如 [EmailFilter-ai-robot](https://github.com/NBHZW/EmailFilter-ai-robot) 或自定义 AI 服务）实现邮件内容的智能解析与分类

```
提前说明
当前版本为Version0.1，使用的KMP+Ktor为初始化项目结构，但是因为还没学习和接触KMP的相关内容，所以暂且没有涉及
至于为何AI功能要和本功能分开写，第一是为了区分功能模块，第二是因为AI功能是补充，之前写过的，直接利用了而已
```



## 技术栈

### 后端

- **核心框架**：Ktor（用于构建 HTTP 服务器，处理路由与请求）
- **邮件处理**：JavaMail（连接 QQ 邮箱 IMAP 服务器，读取与解析邮件）
- **并发处理**：Kotlin 协程（并行解析邮件，提升处理效率）
- **数据序列化**：Kotlinx Serialization 和 GSON（JSON 数据序列化 / 反序列化）
- **缓存管理**：ConcurrentHashMap（线程安全的本地缓存，按日期分区存储）
- **网络通信**：原生 HTTPURLConnection（与外部 AI 服务通信）
- **依赖管理**：Gradle（项目构建与依赖管理）

### 前端

- **UI 框架**：Compose Multiplatform（跨平台 UI 库，支持 Android、桌面、Web）
- **Web 支持**：Kotlin/Wasm（编译为 WebAssembly，运行于浏览器）

### 其他

- **跨平台基础**：Kotlin Multiplatform（共享业务逻辑与数据模型）
- **CORS 支持**：Ktor CORS 插件（处理跨域请求）

## 设计特点

1. **分层架构设计**
   - **路由层**：`EmailRouter` 处理 HTTP 请求，定义 `/email/findAllEmail` 接口。
   - **领域模型**：`EmailEnty`（邮件实体）、`Event`（解析后的面试事件实体）封装核心数据。
   - **缓存层**：`EmailCache` 负责邮件数据的本地缓存与过期管理。
   - **会话层**：`SessionUtil` 管理 QQ 邮箱 IMAP 连接，自动处理连接初始化与关闭。
2. **高效的邮件处理**
   - 基于接收时间（而非发送时间）筛选邮件，避免因发送方时区差异导致的过滤误差。
   - 协程并行解析邮件内容（`processEmailsParallel`），限制最大并行数（10 个）避免资源耗尽。
3. **缓存策略优化**
   - 按邮件接收日期分区存储缓存（`LocalDate` 作为键），支持按时间区间快速查询。
   - 异步更新缓存（`EmailCache.executor`），避免阻塞主请求流程。
4. **可扩展的 AI 集成**
   - 邮件数据通过 `sendPostRequest` 发送至外部 AI 服务，解析结果后更新缓存，便于替换为不同的 AI 服务实现。
5. **资源安全管理**
   - `SessionUtil` 中通过 JVM shutdown hook 自动关闭邮箱连接，避免资源泄漏。
   - 邮件解析过程中处理空值与异常（如日期解析失败时回退到字符串比较），保证程序稳定性。

## 使用说明

1. **环境依赖**
   - JDK 11+
   - Gradle 7.0+
   - QQ 邮箱开启 IMAP 服务并获取授权码（在 QQ 邮箱设置中开启）
2. **配置修改**
   - 在 `SessionUtil` 中替换 `userName`（QQ 邮箱地址）和 `password`（IMAP 授权码）。
   - 若使用自定义 AI 服务，修改 `sendPostRequest` 中的 `url` 为 AI 服务地址。
3. **启动服务**
   - 后端：运行 `Application.kt` 中的 `main` 函数，启动 Ktor 服务器（默认端口 8080）。
   - 前端（Web）：执行 Gradle 任务 `:composeApp:wasmJsBrowserDevelopmentRun`，在浏览器访问前端页面。
4. **AI 服务集成**确保外部 AI 服务可接收 JSON 格式的邮件数据（`List<EmailEnty>`），并返回结构化的面试事件数据（`List<Event>`）。

## 功能亮点

1. **邮箱同步与过滤**自动连接 QQ 邮箱 IMAP 服务器，同步近 5 天的邮件(后续计划修改为传参设置是)，并基于接收时间筛选有效邮件，排除历史过期邮件。
2. **面试信息智能提取**将邮件数据发送至外部 AI 服务，提取关键信息（如公司名称、岗位、面试类型、时间等），结构化存储为 `Event` 实体。
3. **本地缓存优化**采用按日期分区的缓存策略，减少重复请求与 AI 服务调用，提升数据加载效率。（目前缓存的粒度为按天缓存，第一方案是按照分/秒级缓存，但是设计到比较麻烦的时间区间合并与处理，暂且还在研发中)
4. **并行处理提升性能**使用 Kotlin 协程并行解析邮件内容，提高大规模邮件处理的效率。经过初期的测试，使用配套的AI后端前提下，不做任何处理整体需要3min，使用协程优化（后续甚至可以协程+虚拟线程）可以减少到30s。
5. **跨平台支持**前端基于 Compose Multiplatform 开发，支持 Android、JVM 桌面端及 Web（Wasm）平台，提供一致的用户体验。(案例前端目前为Untitled-1.html 所示，还未进行更多的设计)
6. **灵活的 AI 服务集成**通过 HTTP 接口与外部 AI 服务通信，支持自定义 AI 服务地址，便于扩展与替换

## 
