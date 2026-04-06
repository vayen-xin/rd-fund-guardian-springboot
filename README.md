# rd-fund-guardian-springboot

研发费用合规智能管理系统后端项目。

本项目基于 `Spring Boot 3 + Java 17 + MyBatis-Plus + Sa-Token + MySQL`，用于支撑研发费用归集、项目月度汇总、待结算处理、打卡记录导入、账号与日志管理等核心业务。

## 技术栈

- `Spring Boot 3.2.4`
- `Java 17`
- `MyBatis-Plus 3.5.10.1`
- `Sa-Token 1.39.0`
- `MySQL 8`
- `Apache POI`：Excel 导入导出
- `Lombok`

## 核心功能

- 认证与权限
  - 基于 `Sa-Token` 的登录认证
  - 角色：`admin`、`branch_admin`、`user`
  - 登录失败限流

- 基础数据管理
  - 员工管理
  - 设备管理
  - 打卡记录导入、预解析、确认导入

- 项目管理
  - 项目创建、列表、详情、结束
  - 项目关联员工与设备
  - 项目月度费用汇总

- 月度与结算
  - 8 大费用分类及子分类
  - 月度保存、重新编辑
  - 发起待结算、确认结算、重新打开编辑
  - 凭证文件上传、下载、删除

- 系统管理
  - 账号管理
  - 操作日志查询
  - 关键业务审计日志

## 目录结构

```text
src/main/java/com/vayen/rdcm
├─ audit/              审计注解与切面
├─ common/             通用返回与全局异常处理
├─ config/             Spring / MyBatis / Sa-Token 配置
├─ controller/         接口层
├─ dto/                请求与响应对象
├─ entity/             实体类
├─ logging/            SpringBoot 应用日志切面
├─ mapper/             MyBatis-Plus Mapper
├─ security/           当前用户、权限、登录安全
├─ service/            业务接口
├─ service/impl/       业务实现
├─ task/               定时任务
└─ util/               JSON、费用目录等工具类
```

## 本地运行

### 1. 环境要求

- `JDK 17+`
- `Maven 3.6+` 或项目自带 `mvnw.cmd`
- `MySQL 8`

### 2. 数据库准备

默认配置见 [application.yml](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/src/main/resources/application.yml)：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rdcm?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

请先本地创建数据库：

```sql
CREATE DATABASE rdcm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

项目中提供了初始化脚本：

- [schema-v4.sql](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/src/main/resources/schema-v4.sql)
- [data.sql](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/src/main/resources/data.sql)

如果需要，也可以执行 `docs/sql` 目录下的增量脚本。

### 3. 启动项目

推荐使用 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

或使用本机 Maven：

```powershell
mvn spring-boot:run
```

默认启动地址：

- 后端接口：`http://localhost:8080`

## 关键配置

### CORS

前端开发地址通过环境变量控制：

```yaml
app:
  cors:
    allowed-origin-patterns: ${APP_CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:5173,http://127.0.0.1:5173}
```

### 登录限流

```yaml
app:
  security:
    login-limit:
      enabled: true
      max-failures: 8
      window-seconds: 300
      lock-seconds: 900
```

### 文件上传

```yaml
app:
  upload:
    base-dir: ${APP_UPLOAD_BASE_DIR:./uploads}
```

生产环境建议把上传目录挂载到容器外部。

## 默认接口约定

- 登录：`POST /api/auth/login`
- 当前用户：`GET /api/auth/current`
- 退出登录：`POST /api/auth/logout`
- 统一返回：`code / message / data`
- 认证头：`Authorization: Bearer <token>`

## 测试数据

项目根目录提供了简单测试请求样例：

- [testdata/login.json](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/testdata/login.json)
- [testdata/create-project.json](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/testdata/create-project.json)
- [testdata/monthly-save.json](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/testdata/monthly-save.json)
- [testdata/confirm-settlement.json](C:/Users/dell/Desktop/RDCM/rd-fund-guardian/rd-fund-guardian-springboot/testdata/confirm-settlement.json)

## 部署建议

- 使用 `application-prod.yml` 或环境变量区分生产配置
- MySQL 单独部署
- 上传目录挂载到宿主机
- 日志建议通过 Docker / 服务器日志统一收集

## 当前版本说明

当前代码已经可以作为第一版测试版使用，适合：

- 小伙伴联调测试
- 老师演示查看
- 收集甲方反馈前的内部试运行
