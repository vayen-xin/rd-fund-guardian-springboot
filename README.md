# 后端代码说明

## 📦 技术栈

- **Spring Boot**: 4.0.2
- **Java**: 17
- **数据库**: H2 Database (TCP 模式)
- **ORM**: MyBatis（纯 MyBatis 方案）
- **权限认证**: Sa-Token 1.39.0
- **Lombok**: @Data 注解简化代码

---

## 🗂️ 代码结构

```
src/main/
├── java/com/vayen/rdcm/
│   ├── common/              # 公共类
│   │   ├── Result.java                 # 统一响应格式
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   ├── config/              # 配置类
│   │   └── SaTokenConfig.java          # Sa-Token 配置
│   ├── controller/          # Controller 层
│   │   └── AuthController.java         # 认证接口
│   ├── dto/                 # 数据传输对象
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── entity/              # 实体类（10 个）
│   │   ├── UserAccount.java
│   │   ├── Employee.java
│   │   ├── Equipment.java
│   │   ├── ClockInRecord.java
│   │   ├── Project.java
│   │   ├── ProjectEmployee.java
│   │   ├── ProjectEquipment.java
│   │   ├── ProjectSettlement.java
│   │   ├── ExpenseVoucher.java
│   │   └── OperationLog.java
│   ├── mapper/              # MyBatis Mapper 接口
│   │   ├── UserMapper.java
│   │   ├── EmployeeMapper.java
│   │   ├── ProjectMapper.java
│   │   ├── ProjectEmployeeMapper.java
│   │   └── ... (后续补充)
│   └── RdcmApplication.java # 启动类
└── resources/
    ├── mapper/              # MyBatis XML Mapper
    │   ├── UserMapper.xml
    │   ├── EmployeeMapper.xml
    │   ├── ProjectMapper.xml
    │   ├── ProjectEmployeeMapper.xml
    │   └── ... (后续补充)
    ├── data.sql             # 数据库初始化脚本
    └── application.yml      # 配置文件
```

---

## 💡 数据库操作方式

本项目采用**纯 MyBatis**方案：

```java
@Autowired
private EmployeeMapper employeeMapper;

// 查询所有启用员工
List<Employee> employees = employeeMapper.findAllActive();

// 保存员工
Employee emp = new Employee();
emp.setName("张三");
emp.setType("正式");
employeeMapper.insert(emp);

// 批量操作
employeeMapper.batchInsert(list);
```

**优势**:
- 你熟悉 MyBatis，上手快
- SQL 灵活，便于优化
- 便于后续复杂查询和动态 SQL

---

## 🚀 本地测试步骤

### 1. 环境要求
- JDK 17+
- Maven 3.6+

### 2. 启动项目
```bash
cd /root/.openclaw/workspace/projects/rd-fund-guardian/backend
mvn spring-boot:run
```

### 3. 访问 H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:tcp://localhost:9092/./data/rdcm_db`
- 用户名：sa
- 密码：空

### 4. 测试 API

```bash
# 1. 登录获取 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 响应：{"code":200,"data":{"token":"Bearer xxx","userId":1}}

# 2. 使用 token 访问受保护接口
curl http://localhost:8080/api/auth/current \
  -H "Authorization: Bearer <your-token>"

# 3. 登出
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <your-token>"
```

---

## 📝 已实现功能

### ✅ 数据库设计
- 10 张表（无外键约束）
- JSON 字段支持（凭证文件、操作日志）
- 索引优化

### ✅ 实体类（10 个）
- 全部使用 @Data 注解
- 自动时间戳（@PrePersist）

### ✅ MyBatis Mapper（4 个）
- UserMapper（用户）
- EmployeeMapper（员工）
- ProjectMapper（项目）
- ProjectEmployeeMapper（项目 - 员工关联）
- XML 配置 + 接口注解

### ✅ Sa-Token 权限认证
- 依赖引入（1.39.0）
- 配置类（拦截器）
- 登录/登出接口
- 全局异常处理
- 统一响应格式

### ✅ 初始化脚本
- data.sql（建表 + 测试数据）
- 应用启动自动执行

---

## ⏭️ 待开发功能

1. **剩余 Mapper**
   - EquipmentMapper
   - ClockInRecordMapper
   - ProjectSettlementMapper
   - ExpenseVoucherMapper
   - OperationLogMapper

2. **Service 层**
   - 业务逻辑封装
   - 事务管理（@Transactional）

3. **Controller 层**
   - 员工管理 API
   - 设备管理 API
   - 项目管理 API
   - 结算管理 API

4. **文件上传**
   - 凭证文件上传
   - 存储路径管理

5. **审计包生成**
   - PDF 报表生成
   - ZIP 打包下载

---

## ⚠️ 注意事项

1. **Lombok 插件**: IDEA 需安装 Lombok 插件
2. **H2 数据持久化**: 数据保存在 `./data/rdcm_db`
3. **端口占用**: 确保 8080 和 9092 端口未被占用
4. **字符编码**: 数据库使用 UTF-8
5. **密码加密**: 目前使用明文对比，后续需改为 BCrypt
6. **Token 认证**: 请求头需带 `Authorization: Bearer <token>`

---

## 📖 相关文档

- [数据库设计文档.md](./数据库设计文档.md)
- [Sa-Token 使用指南.md](./Sa-Token 使用指南.md)
- [功能需求文档](../../研发项目资金合规管理 - 开发功能文档 2.0.docx)

---

**更新时间**: 2026-03-07  
**版本**: V2.0（纯 MyBatis + Sa-Token）
