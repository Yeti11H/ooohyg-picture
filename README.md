# ☁️ 企业级智能协同云图库 (Cloud Picture Platform)

> 基于 Spring Boot + Vue3 的企业级图片协作平台，集成 AIGC 智能扩图与多级缓存架构。

**🔗 在线体验地址：** [http://129.204.154.139](http://129.204.154.139)  
*(测试账号：ooohyg / 12345678)*

---

## 🛠️ 技术栈 (Tech Stack)

- **后端**：Spring Boot 2.7, MyBatis Plus
- **数据库**：MySQL 8.0 (主从架构设计)
- **缓存**：Caffeine (本地) + Redis (分布式) 多级缓存
- **消息队列**：RabbitMQ (异步解耦/流量削峰)
- **AI 能力**：阿里通义万相 (DashScope SDK)
- **存储**：腾讯云 COS + 数据万象 (CI)
- **运维**：Docker, Nginx, Linux

---

## 🌟 核心亮点 (Highlights)

### 1. 🎨 AIGC 智能扩图与风格重绘
- 针对 AI 生成耗时（10s+）痛点，设计了 **"生产-消费"异步任务架构**。
- 前端提交任务 -> 写入 DB (Pending) -> 发送 MQ -> 消费者调用 AI -> 轮询查状态。
- 解决了 **跨云厂商格式兼容性** 问题（WebP 转 PNG），利用 COS 数据万象实现云端实时转码。

### 2. 🚀 多级缓存防击穿架构
- 针对热点图片查询，构建 **Caffeine + Redis** 两级缓存体系。
- 引入 **Redisson 分布式锁** 解决缓存击穿问题，采用 Double-Check Lock 机制保障数据库安全。

### 3. 🛡️ 企业级权限与安全
- 基于 **AOP 切面** 实现精细化 RBAC 权限控制（@SaSpaceCheckPermission）。
- 实现了基于 **事务 (@Transactional)** 的批量操作（删/改/审），杜绝脏数据。

### 4. 🐳 DevOps 容器化部署
- 编写 Dockerfile 优化 JVM 参数 (`-Xmx512m`)，在低配服务器上稳定运行。
- 使用 Nginx 反向代理实现前后端同域访问，解决 CORS 跨域问题。

---

## 📂 目录结构 (Structure)

```text
com.h.ooohygpicture
├── aop             // 切面 (权限校验/日志)
├── config          // 配置类 (Cors/MyBatis/Redis)
├── controller      // 接口层
├── manager         // 通用模块 (AI调用/COS上传)
├── service         // 业务逻辑层 (核心)
└── listen          // MQ 监听器
https://www.google.com/url?q=https%3A%2F%2Fgithub.com%2FYeti11H%2Fooohyg-picture
