# Sa-Token 权限认证使用指南

## 📦 依赖信息

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.39.0</version>
</dependency>
```

**官方文档**: https://sa-token.cc/

---

## ⚙️ 配置说明

### application.yml 配置

```yaml
sa-token:
  token-name: Authorization          # token 名称
  timeout: 2592000                   # token 有效期 30 天
  is-concurrent: true                # 允许同一账号并发登录
  is-share: false                    # 每次登录新建 token
  token-style: uuid                  # token 风格
  is-read-header: true               # 从 header 读取 token
  token-prefix: Bearer               # token 前缀
```

### 请求头格式

```
Authorization: Bearer <your-token>
```

---

## 🚀 快速开始

### 1. 登录（生成 token）

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        // 1. 验证用户名密码（从数据库查询）
        UserAccount user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return Result.error("用户名或密码错误");
        }
        
        // 2. 登录，生成 token
        StpUtil.login(user.getId());
        
        // 3. 返回 token
        String token = StpUtil.getTokenValue();
        return Result.success(new LoginResponse(user.getUsername(), token));
    }
    
    @PostMapping("/logout")
    public Result logout() {
        StpUtil.logout();
        return Result.success();
    }
}
```

### 2. 权限校验（拦截器自动处理）

配置类 `SaTokenConfig.java` 已设置：
- 拦截所有路径 `/**`
- 排除登录/注册接口 `/api/auth/login`, `/api/auth/register`
- 排除 H2 Console `/h2-console/**`

**未登录访问受保护接口会返回 401**

### 3. 手动校验登录状态

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @GetMapping
    public Result list() {
        // 校验登录（如果已配置拦截器，这里可以省略）
        StpUtil.checkLogin();
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<Project> projects = projectMapper.findAll();
        return Result.success(projects);
    }
}
```

---

## 🔐 常用 API

### 登录/注销

```java
// 登录
StpUtil.login(userId);

// 注销
StpUtil.logout();

// 注销指定账号
StpUtil.logout(userId);
```

### 查询登录状态

```java
// 是否已登录
boolean isLogin = StpUtil.isLogin();

// 获取当前登录用户 ID
Object userId = StpUtil.getLoginId();

// 获取当前登录用户 ID（Long 类型）
long userId = StpUtil.getLoginIdAsLong();

// 获取 token
String token = StpUtil.getTokenValue();

// token 剩余有效期（秒）
long timeout = StpUtil.getTokenTimeout();
```

### 角色权限（可选功能）

```java
// 授予角色
StpUtil.setRole(userId, "admin");
StpUtil.setRole(userId, "user");

// 判断是否有角色
boolean hasRole = StpUtil.hasRole(userId, "admin");

// 权限校验
StpUtil.checkRole("admin");

// 授予权限
StpUtil.setPermission(userId, "project:add");
StpUtil.setPermission(userId, "project:delete");

// 判断是否有权限
boolean hasPerm = StpUtil.hasPermission(userId, "project:add");

// 权限校验
StpUtil.checkPermission("project:add");
```

---

## 📝 实战示例

### 用户登录请求

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "username": "admin",
    "token": "Bearer 8f7e9d6c-5b4a-3e2d-1c0b-9a8f7e6d5c4b"
  }
}
```

### 访问受保护接口

```bash
curl http://localhost:8080/api/projects \
  -H "Authorization: Bearer 8f7e9d6c-5b4a-3e2d-1c0b-9a8f7e6d5c4b"
```

---

## ⚠️ 注意事项

### 1. 密码加密

**务必使用密码加密**，不要明文存储！

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 注册时加密
String encodedPassword = passwordEncoder.encode(rawPassword);

// 登录时验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### 2. 全局异常处理

Sa-Token 校验失败会抛出异常，需要全局异常处理器：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 处理 Sa-Token 未登录异常
    @ExceptionHandler(NotLoginException.class)
    public Result handleNotLogin(NotLoginException e) {
        return Result.error(401, "未登录或 token 已过期");
    }
    
    // 处理 Sa-Token 无权限异常
    @ExceptionHandler(NotPermissionException.class)
    public Result handleNotPermission(NotPermissionException e) {
        return Result.error(403, "无权限访问");
    }
}
```

### 3. Token 刷新

可以在每次请求时自动刷新 token 有效期：

```java
// 在拦截器中
SaRouter.match("/**").check(r -> {
    StpUtil.checkLogin();
    StpUtil.extendTimeout(1800); // 延长 30 分钟
});
```

---

## 📚 进阶功能

### 1. 多端登录

```java
// 指定设备类型登录
StpUtil.login(userId, "APP");
StpUtil.login(userId, "WEB");
StpUtil.login(userId, "PC");

// 校验指定端登录
StpUtil.checkLogin("APP");
```

### 2. 记住我

```java
// 登录并记住我（token 永久有效）
StpUtil.login(userId, isRemember);
```

### 3. 临时身份切换

```java
// 临时切换为其他用户操作
StpUtil.switchTo(userId);
// ... 执行操作
StpUtil.endSwitch();
```

---

## 🔧 后续开发计划

1. ✅ Sa-Token 依赖引入
2. ✅ Sa-Token 配置类
3. ⏭️ 用户登录/注册接口
4. ⏭️ 全局异常处理器
5. ⏭️ 统一响应格式 Result
6. ⏭️ 密码加密 BCrypt
7. ⏭️ 角色权限管理（可选）

---

**文档版本**: V1.0  
**更新时间**: 2026-03-07  
**Sa-Token 版本**: 1.39.0
