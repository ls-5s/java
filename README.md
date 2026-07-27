# 苍穹外卖 (Sky Take-Out)

基于 Spring Boot 的外卖点餐管理系统。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.3 | 基础框架 |
| MyBatis + MyBatis Plus | ORM 持久层 |
| MySQL 8.0 | 关系型数据库 |
| Redis | 缓存 |
| Druid | 数据库连接池 |
| JWT (jjwt) | 用户认证 |
| Knife4j | API 文档 |
| 阿里云 OSS | 对象存储 |
| 微信小程序支付 | 支付接入 |
| Apache POI | Excel 报表导出 |
| WebSocket | 实时消息推送 |
| AOP | 自动填充/日志切面 |
| Lombok | 代码简化 |

## 项目结构

```
sky-take-out
├── sky-common      # 公共模块：常量、异常、工具类、通用返回结果
├── sky-pojo         # 实体模块：DTO、Entity、VO
└── sky-server       # 服务模块：Controller、Service、Mapper、配置
```

## 功能模块

### 管理端
- 员工管理（登录/登出、CRUD）
- 分类管理
- 菜品管理
- 套餐管理
- 订单管理
- 数据统计与报表导出
- 店铺状态管理

### 用户端
- 微信登录
- 菜品浏览
- 购物车
- 下单与支付
- 地址管理
- 订单查询

## 环境配置

运行前需配置以下环境变量：

```bash
ALIOSS_ACCESS_KEY_ID      # 阿里云 AccessKey ID
ALIOSS_ACCESS_KEY_SECRET  # 阿里云 AccessKey Secret
WECHAT_APPID              # 微信 AppID
WECHAT_SECRET             # 微信 Secret
```

## 快速开始

```bash
# 克隆项目
git clone git@github.com:ls-5s/java.git
cd sky-take-out

# 编译
mvn clean package -DskipTests

# 启动
cd sky-server
mvn spring-boot:run
```

启动后访问:
- 接口文档：`http://localhost:8080/doc.html`
- 管理端：`http://localhost:8080/admin/index.html`
