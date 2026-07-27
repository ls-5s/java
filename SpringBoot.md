# SpringBoot 核心知识点整理

---

## 一、SpringBoot 整合 JUnit5

### 1. 依赖（SpringBoot 项目自带，无需额外添加）
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>  <!-- 仅测试环境生效，打包不带 -->
</dependency>
```

### 2. 目录规范（必须遵守）
测试类包路径必须和启动类一致，否则注入 Bean 为 null。

### 3. 基础单元测试
```java
// 加载Spring完整容器，自动注入所有Service/Mapper
@SpringBootTest
class DemoApplicationTest {
    @Autowired
    private UserService userService; // 注入要测试的业务类

    @Test // 右键可单独执行此方法
    void testQueryUser() {
        // 1. 调用业务方法
        var user = userService.getById(1L);
        // 2. 断言校验（不符合直接报错，测试失败）
        Assertions.assertNotNull(user);                        // 判断查询结果不为空
        Assertions.assertEquals("admin", user.getUserName());  // 判断用户名等于admin
    }
}
```

### 4. 前置/后置方法（初始化和清理数据）
```java
@BeforeEach // 每个 @Test 执行前运行
void before() {
    System.out.println("测试开始，初始化数据");
}

@AfterEach  // 每个 @Test 执行完成后运行
void after() {
    System.out.println("测试结束，清理测试数据");
}
```

### 5. MockMvc 测试 Controller（不启动Tomcat端口）
无需启动服务器端口，直接模拟GET/POST请求测试接口逻辑：
```java
@SpringBootTest
class DemoApplicationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApi() throws Exception {
        // 模拟GET请求，断言返回码200
        mockMvc.perform(get("/api/user/1"))
               .andExpect(status().isOk());
    }
}
```

### 6. 仅加载数据库层（@MyBatisTest，启动更快）
```java
@MyBatisTest // 只加载 MyBatis + 数据源，不加载全部 Bean
class UserMapperTest {
    @Autowired
    private UserMapper userMapper;
}
```

### 7. 事务自动回滚（测试不污染数据库）
```java
@SpringBootTest
@Transactional // 测试结束自动回滚，不会修改真实数据
class DemoApplicationTest { }
```

### 8. 常见坑
| 问题 | 原因 |
|------|------|
| Service 注入为 null | 测试类包路径和启动类不一致 / 未加 @SpringBootTest |
| @Test 标红报错 | 导错包，JUnit5 正确包：`org.junit.jupiter.api.Test` |
| 测试方法无法运行 | 方法有返回值 / 有参数 / 加了 private |
| 数据库数据被污染 | 未加 @Transactional 自动回滚 |

---

## 二、@SpringBootTest 的 classes 属性（优化测试启动速度）

### 核心作用
- `@SpringBootTest`（无参数）：自动扫描**整个项目**所有Bean，项目越大启动越慢
- `@SpringBootTest(classes = 启动类.class)`：**只加载指定类**的Spring上下文，启动速度大幅提升

```java
// 写法1：加载全部Bean，慢
@SpringBootTest
public class TestControllerTest {}

// 写法2：只加载指定启动类上下文，快（本节重点）
@SpringBootTest(classes = Demo1Application.class)
public class TestControllerTest {}

// 写法3：数组形式指定多个类，只加载你需要的Bean
@SpringBootTest(classes = {Demo1Application.class, UserService.class})
public class TestControllerTest {}
```

### 注意点
- classes 里必须包含**启动类**，否则Spring无法扫描Controller/Service
- 用到 Mapper/数据库时，需把相关配置类也加入 classes
- 大型企业项目差距明显，小型练习项目感知不强

---

## 三、SpringBoot 整合 MyBatis

### 1. 核心依赖
```xml
<!-- MyBatis 整合 SpringBoot 启动器 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope> <!-- 运行时才需要，编译期不需要 -->
</dependency>
```

### 2. application.yml 核心配置
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver  # MySQL 8.x 驱动类
    url: jdbc:mysql://127.0.0.1:3306/test_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root

mybatis:
  type-aliases-package: com.xxx.entity        # 实体类别名包，Mapper中不用写完整类名
  mapper-locations: classpath:mapper/**/*.xml  # XML映射文件路径
  configuration:
    map-underscore-to-camel-case: true         # 驼峰自动映射：user_name → userName
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 控制台打印SQL
```

### 3. 两种 SQL 编写方式

**注解版（简单单表CRUD，不用XML）：**
```java
@Mapper // 交给MyBatis代理生成实现类
public interface UserMapper {
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User selectById(Integer id);

    // 多参数用 @Param 绑定参数名
    @Select("SELECT * FROM t_user WHERE user_name = #{name} AND age = #{age}")
    User selectByNameAge(@Param("name") String userName, @Param("age") Integer age);

    @Insert("INSERT INTO t_user(user_name,age,email) VALUES(#{userName},#{age},#{email})")
    int insert(User user);
}
```

**XML 版（复杂多表联查、动态SQL推荐，路径 resources/mapper/UserXmlMapper.xml）：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace 绑定对应 Mapper 接口全类名 -->
<mapper namespace="com.xxx.mapper.UserXmlMapper">

    <!-- 查询全部，resultType 指向实体类别名 -->
    <select id="selectAllUser" resultType="User">
        SELECT id, user_name, age, email FROM t_user
    </select>

    <!-- 动态SQL：<where> + <if> 实现多条件组合 -->
    <select id="selectByCondition" resultType="User">
        SELECT * FROM t_user
        <where>
            <if test="userName != null and userName != ''">
                AND user_name LIKE CONCAT('%', #{userName}, '%')
            </if>
            <if test="age != null">
                AND age = #{age}
            </if>
        </where>
    </select>
</mapper>
```

### 4. 启动类 + @MapperScan（企业项目首选）
```java
@SpringBootApplication
@MapperScan("com.xxx.mapper") // 批量扫描mapper包，无需每个接口单独加 @Mapper
public class MybatisDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MybatisDemoApplication.class, args);
    }
}
```

### 5. 关键知识点
| 知识点 | 说明 |
|--------|------|
| `#{}` vs `${}` | `#{}` 预编译防注入（推荐）；`${}` 字符串拼接有注入风险，仅用于动态表名/排序字段 |
| @Mapper vs @MapperScan | `@Mapper` 单个标注；`@MapperScan` 启动类统一扫描（企业首选） |
| @Transactional | Service 层加此注解，异常自动回滚 |
| Mapper.xml 找不到 | 检查 `mapper-locations` 路径 + pom.xml 是否配置 resources 过滤 |

---

## 四、SpringBoot 整合 MyBatis-Plus（MP）

### 1. 核心概念
MyBatis-Plus 是 MyBatis 的**增强工具**，完全兼容原生 MyBatis。内置通用 CRUD、分页插件、条件构造器，**单表操作不用手写SQL**。

| 对比 | 原生 MyBatis | MyBatis-Plus |
|------|-------------|--------------|
| 基础 CRUD | 必须手写 SQL（XML/注解） | 内置方法，零 SQL |
| 分页 | 手动引入 PageHelper | 内置分页插件 |
| 条件查询 | 手写 `<if>` 动态 SQL | LambdaQueryWrapper 链式调用 |
| 主键策略 | 手动配置自增 | 内置多种策略（自增、雪花ID等） |
| 逻辑删除 | 手写 UPDATE | 全局配置，自动实现 |

> MP 底层仍是 MyBatis，原有 XML/注解 SQL 完全兼容，可混用。MyBatis 适合复杂多表联查，MP 适合单表快速开发。

### 2. 核心依赖
```xml
<!-- 替代 mybatis-spring-boot-starter，不要同时引入 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
```

### 3. application.yml
```yaml
mybatis-plus:
  type-aliases-package: com.xxx.entity        # 实体类别名包
  mapper-locations: classpath:mapper/**/*.xml  # XML映射文件路径（兼容MyBatis）
  configuration:
    map-underscore-to-camel-case: true         # 驼峰自动映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印SQL（仅开发环境开启）
  global-config:
    db-config:
      id-type: auto                    # 主键策略：auto = 自增
      logic-delete-field: isDeleted    # 逻辑删除字段名
      logic-delete-value: 1            # 已删除的值
      logic-not-delete-value: 0        # 未删除的值
```

### 4. 实体类（MP + Lombok 标准写法）
```java
@Data
@NoArgsConstructor   // 无参构造（MyBatis 反射实例化必须）
@AllArgsConstructor  // 全参构造（方便测试快速创建）
@TableName("t_user") // 绑定数据库表名（类名和表名不一致时必须加）
public class User {
    @TableId(type = IdType.AUTO) // 主键，自增策略
    private Integer id;

    private String userName; // 开启驼峰映射后，自动对应数据库 user_name

    private Integer age;
    private String email;

    @TableLogic // 标记逻辑删除字段（删除时自动改为 UPDATE is_deleted=1）
    private Integer isDeleted;
}
```

### 5. Mapper 层（继承 BaseMapper，零SQL搞定单表CRUD）
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 后，自动获得以下方法，无需手写：
    // insert(entity)    — 新增
    // deleteById(id)    — 根据id删除（配置了逻辑删除则自动变成UPDATE）
    // updateById(entity)— 根据id修改
    // selectById(id)    — 根据id查询
    // selectList(null)  — 查询全部
    // selectPage(page, wrapper) — 分页查询
    //
    // 复杂多表联查仍可在此定义方法 + XML 映射文件
}
```

### 6. Service 层（继承 IService / ServiceImpl，90% CRUD 无需自己写）
```java
// Service 接口 — 只声明自定义查询方法，基础 CRUD 由 IService 提供
public interface UserService extends IService<User> {
    List<User> getByCondition(String username, Integer gender, Integer minAge);
    IPage<User> getPage(Long current, Long size, String username);
}

// Service 实现 — 继承 ServiceImpl，自动拥有 save/remove/update/get/list/page 等方法
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    // ===== 只有多条件复杂查询才需要自己实现 =====

    @Override
    public List<User> getByCondition(String username, Integer gender, Integer minAge) {
        // 动态条件查询：前端传什么就拼什么，不传就不拼
        return userMapper.selectList(
            Wrappers.lambdaQuery(User.class)
                .like(username != null, User::getUsername, username)   // username不为null才拼接模糊
                .eq(gender != null, User::getGender, gender)           // gender不为null才拼接等值
                .ge(minAge != null, User::getAge, minAge)              // minAge不为null才拼接大于等于
                .orderByDesc(User::getId)                              // 按id倒序
        );
    }

    @Override
    public IPage<User> getPage(Long current, Long size, String username) {
        Page<User> page = new Page<>(current, size); // 页码、每页条数
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
                .like(username != null, User::getUsername, username);
        return userMapper.selectPage(page, wrapper);
    }
}
```

**Service 父类自带的高频方法（不需要自己实现）：**

| 方法 | 功能 |
|------|------|
| `this.save(entity)` | 新增 |
| `this.getById(id)` | 根据主键查单条 |
| `this.list()` | 查询全表 |
| `this.list(wrapper)` | 按条件查询列表 |
| `this.updateById(entity)` | 根据主键修改 |
| `this.removeById(id)` | 根据主键删除（逻辑删除则改为UPDATE） |
| `this.page(page, wrapper)` | 分页查询 |

### 7. 条件构造器 LambdaQueryWrapper（开发核心）

#### 为什么用 LambdaQueryWrapper
- `QueryWrapper`（不推荐）：字符串硬编码字段名 `"user_name"`，容易写错
- `LambdaQueryWrapper`（企业标准）：用实体 getter 方法引用 `User::getUserName`，编译期校验，杜绝拼写错误

#### 快速创建方式
```java
// 方式1：new 创建
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

// 方式2：Wrappers 工具类创建（推荐）
LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class);
```

#### 常用条件方法

| 方法 | SQL 作用 | 示例 |
|------|----------|------|
| `eq(字段, 值)` | = 等于 | `.eq(User::getGender, 1)` → gender = 1 |
| `ne(字段, 值)` | <> 不等于 | `.ne(User::getStatus, 0)` → status <> 0 |
| `gt(字段, 值)` | > 大于 | `.gt(User::getAge, 18)` → age > 18 |
| `ge(字段, 值)` | >= 大于等于 | `.ge(User::getScore, 60)` → score >= 60 |
| `lt(字段, 值)` | < 小于 | `.lt(User::getAge, 60)` → age < 60 |
| `le(字段, 值)` | <= 小于等于 | `.le(User::getPrice, 100)` → price <= 100 |
| `like(字段, 值)` | LIKE '%值%' 全模糊 | `.like(User::getUsername, "张")` → username LIKE '%张%' |
| `likeRight(字段, 值)` | LIKE '值%' 右模糊 | `.likeRight(User::getPhone, "139")` → phone LIKE '139%' |
| `between(字段, v1, v2)` | BETWEEN v1 AND v2 | `.between(Score::getScore, 60, 100)` |
| `in(字段, 集合)` | IN (...) | `.in(User::getId, Arrays.asList(1L, 2L, 3L))` |
| `isNull(字段)` | IS NULL | `.isNull(User::getPhone)` |
| `isNotNull(字段)` | IS NOT NULL | `.isNotNull(User::getUsername)` |
| `orderByAsc(字段)` | ORDER BY ASC | `.orderByAsc(User::getId)` |
| `orderByDesc(字段)` | ORDER BY DESC | `.orderByDesc(User::getId)` |

#### 动态条件查询（重要：带布尔参数的条件方法）
每个条件方法**第一个参数支持布尔值**，true 才拼接该条件，用于实现"前端不传参就不过滤"：
```java
// 缩略写法：条件判空和方法调用写在一行
return userMapper.selectList(
    Wrappers.lambdaQuery(User.class)
        .like(username != null, User::getUsername, username)  // username 为 null 则不拼接
        .eq(gender != null, User::getGender, gender)          // gender 为 null 则不拼接
        .ge(minAge != null, User::getAge, minAge)             // minAge 为 null 则不拼接
);
```

#### and/or 嵌套条件（括号分组）
```java
// 需求：age < 18  OR  gender = 2
LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
    .and(wr -> wr.lt(User::getAge, 18).or().eq(User::getGender, 2));
// 生成SQL：WHERE (age < 18 OR gender = 2)
```

### 8. 分页功能

#### 分页插件配置（必须！否则分页不生效，会查出全部数据）
```java
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

#### 无条件分页
```java
// Service 层
public IPage<User> getPage(Long current, Long size) {
    Page<User> page = new Page<>(current, size); // current=页码，size=每页条数
    return this.page(page); // 等价于 baseMapper.selectPage(page, null)
}
```

#### 带条件分页（列表查询标配）
```java
// Service 层：条件 + 分页 同时使用
public IPage<User> pageSearch(Long current, Long size, String username, Integer minAge) {
    // 分页参数矫正，防止负数
    current = current < 1 ? 1 : current;
    size = size < 1 ? 10 : size;

    Page<User> page = new Page<>(current, size);
    LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
            .like(username != null, User::getUsername, username)
            .ge(minAge != null, User::getAge, minAge)
            .orderByDesc(User::getId);
    return baseMapper.selectPage(page, wrapper);
}

// Controller 层
@GetMapping("/search")
public R<Page<User>> search(
        @RequestParam(defaultValue = "1") Long current,      // 当前页，默认1
        @RequestParam(defaultValue = "10") Long size,        // 每页条数，默认10
        @RequestParam(required = false) String username,     // 非必传
        @RequestParam(required = false) Integer minAge       // 非必传
) {
    return R.ok(userService.pageSearch(current, size, username, minAge));
}
```

#### Page 分页结果常用属性
```json
{
    "records": [{ "id": 1, "username": "张三" }],  // 当前页数据列表
    "total": 36,   // 数据库总条数
    "pages": 8,    // 总页数
    "current": 1,  // 当前页码
    "size": 5      // 每页条数
}
```

#### 分页常见 BUG
| BUG | 原因 | 修复 |
|-----|------|------|
| 分页不生效，查出全表 | 未配置分页插件 | 必须配置 `MybatisPlusInterceptor` |
| 删除后总数不变 | 前端缓存了旧数据 | 删除成功后重新请求分页接口 |
| 最后一页删完变空白 | 当前页码 > 实际总页数 | 前端判断 `current > pages` 时跳到 pages 页 |
| 分页参数传负数报错 | current=-1, size=0 | 后端矫正：`current = current < 1 ? 1 : current` |
| 删除不存在的id无提示 | 未判断 removeById 返回值 | removeById 返回 false 时返回 `R.fail("数据不存在")` |

### 9. 三层继承模板总结（所有实体通用）
```
Mapper：       extends BaseMapper<T>
Service接口：  extends IService<T>
ServiceImpl：  extends ServiceImpl<M extends BaseMapper<T>, T>  implements  自定义Service接口
```
- 90% 简单 CRUD **无需手写SQL、无需实现**，直接调用父类方法
- 只有**多条件复杂查询**时才写 LambdaQueryWrapper 自定义实现
- 条件拼接逻辑统一写在 **Service 层**，Controller 只负责接收参数

---

## 五、Controller 层标准开发

### 1. 表现层职责
- 接收前端 HTTP 请求（GET/POST/PUT/DELETE）
- 接收参数（路径参数、URL参数、JSON请求体）
- 调用 Service 层，**不写数据库查询和业务逻辑**
- 返回结果给前端

### 2. 核心注解速查

| 注解 | 作用 | 示例 |
|------|------|------|
| `@RestController` | 标识为 REST 控制器，方法返回值自动转 JSON | 类级别 |
| `@RequestMapping("/api/user")` | 统一接口前缀，类下所有接口共用 | 类级别 |
| `@GetMapping("/{id}")` | GET 查询请求 | 查数据用 |
| `@PostMapping` | POST 新增请求 | 提交数据用 |
| `@PutMapping` | PUT 修改请求 | 更新数据用 |
| `@DeleteMapping("/{id}")` | DELETE 删除请求 | 删除数据用 |
| `@PathVariable` | 取 URL 路径上的参数 | `/api/user/6` → 取到 6 |
| `@RequestParam` | 取 URL 问号后的参数 | `/api/user?name=张三` → 取到 "张三" |
| `@RequestBody` | 取 POST/PUT 请求体中的 JSON | `{"username":"张三"}` → 映射为对象 |

### 3. 完整 Controller 示例（增删改查 + 条件分页）
```java
@Slf4j                       // Lombok 日志，替代 LoggerFactory
@RestController              // 标识为 REST 控制器，返回 JSON
@RequestMapping("/api/user") // 统一前缀
public class UserController {

    @Resource // 注入 Service，@Autowired 效果一致
    private UserService userService;

    // ==================== 新增 POST ====================
    @PostMapping
    public R<Boolean> add(@RequestBody User user) { // @RequestBody 接收前端JSON对象
        log.info("新增用户，username={}", user.getUsername());
        boolean saved = userService.save(user);     // 调用父类自带 save 方法
        if (!saved) {
            return R.fail("新增用户失败");
        }
        return R.ok(saved);
    }

    // ==================== 根据id查询 GET ====================
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) { // @PathVariable 取路径参数
        log.debug("查询id={}的用户", id);
        User user = userService.getById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok(user);
    }

    // ==================== 条件分页查询 GET ====================
    @GetMapping("/search")
    public R<Page<User>> search(
            @RequestParam(defaultValue = "1") Long current,  // 当前页，默认1
            @RequestParam(defaultValue = "10") Long size,    // 每页条数，默认10
            @RequestParam(required = false) String username, // required=false 表示非必传
            @RequestParam(required = false) Integer minAge
    ) {
        return R.ok(userService.pageSearch(current, size, username, minAge));
    }

    // ==================== 修改 PUT ====================
    @PutMapping
    public R<Boolean> update(@RequestBody User user) {
        log.info("修改用户，id={}", user.getId());
        boolean updated = userService.updateById(user);
        if (!updated) {
            return R.fail("修改失败，用户不存在");
        }
        return R.ok(updated);
    }

    // ==================== 删除 DELETE ====================
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        log.info("删除用户，id={}", id);
        boolean removed = userService.removeById(id); // 逻辑删除：UPDATE is_deleted=1
        if (!removed) {
            return R.fail("删除失败，用户不存在");
        }
        return R.ok("删除成功");
    }

    // ==================== 查询全部 GET ====================
    @GetMapping("/all")
    public R<List<User>> listAll() {
        return R.ok(userService.list());
    }
}
```

---

## 六、统一返回结果 R 对象

### 痛点
接口直接返回 Boolean / 实体 / List，格式不统一，前端无法统一判断成功/失败。

### 统一返回类 R.java
```java
@Data
public class R<T> {
    private Integer code; // 状态码：200 成功，其他 失败
    private String msg;   // 提示信息
    private T data;       // 业务数据（对象 / 集合 / 布尔 / 分页）

    // 成功，带数据
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    // 成功，无数据（如删除成功）
    public static <T> R<T> ok() {
        return ok(null);
    }

    // 失败，自定义提示（默认200以外的code）
    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

    // 失败，自定义code+msg（配合自定义业务异常使用）
    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }
}
```

### 规范
- Controller 所有接口返回值**统一为 `R<T>`**，T 对应业务数据类型
- 返回示例：
```json
// 成功
{ "code": 200, "msg": "操作成功", "data": { "id": 1, "username": "张三" } }

// 失败
{ "code": 500, "msg": "用户不存在", "data": null }
```

---

## 七、全局异常处理

### 核心注解
| 注解 | 作用 |
|------|------|
| `@RestControllerAdvice` | 拦截所有 `@RestController` 抛出的异常，自动转 JSON |
| `@ExceptionHandler(异常类.class)` | 指定捕获哪种异常，方法内统一返回 `R.fail()` |

### 完整代码
```java
@RestControllerAdvice // 不要用 @ControllerAdvice（不会自动转JSON）
public class GlobalExceptionHandler {

    // 1. 自定义业务异常（优先捕获，精确匹配）
    @ExceptionHandler(BusinessException.class)
    public R<String> handleBusinessException(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    // 2. 兜底捕获所有未知异常（放在最后）
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        e.printStackTrace(); // 打印堆栈，方便排查
        return R.fail("服务器内部异常：" + e.getMessage());
    }
}
```

### 自定义业务异常类
```java
@Data
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
```

### Service 层使用
```java
public User getById(Long id) {
    User user = baseMapper.selectById(id);
    if (user == null) {
        throw new BusinessException(10001, "用户不存在"); // 主动抛业务异常
    }
    return user;
}
// 前端收到：{ "code": 10001, "msg": "用户不存在", "data": null }
```

### 注意点
1. 用 `@RestControllerAdvice`，别用 `@ControllerAdvice`
2. 放在主启动类同级或子包下，确保被 Spring 扫描到
3. 细分异常写上方，通用 Exception 放最下方兜底
4. Controller 层无需再写 try-catch

---

## 八、SpringBoot 整合 Druid 连接池

### 1. 核心概念
阿里开源数据库连接池，替代 SpringBoot 默认 HikariCP。职责：**管理数据库连接**，内置 SQL 监控面板、防 SQL 注入、慢 SQL 记录。

层级关系：
```
Controller → Service → Mapper（MyBatis/MP）
                ↓
          Druid 连接池（管理连接）
                ↓
            MySQL
```

| 对比 | HikariCP | Druid |
|------|---------|-------|
| 定位 | SpringBoot 默认 | 阿里出品 |
| 性能 | 极致高性能 | 性能优异 |
| 监控 | 无监控功能 | 内置 SQL 监控面板 |
| 安全 | 无 | 内置防 SQL 注入 |
| 适用 | 简单项目 | 企业项目首选 |

### 2. 核心依赖
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.16</version>
</dependency>
```

### 3. application.yml 完整配置
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # 指定连接池为 Druid
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/test_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root

    druid:
      # ===== 连接池核心参数 =====
      initial-size: 5        # 初始化连接数
      max-active: 20         # 最大活跃连接数（并发上限，防止数据库连接耗尽）
      min-idle: 5            # 最小空闲连接数
      max-wait: 60000        # 获取连接最大等待时间（毫秒）

      # ===== 过滤器 =====
      filters: stat,wall,log4j2  # stat=SQL监控 / wall=SQL防火墙 / log4j2=日志

      # ===== 监控面板 =====
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*        # 访问地址：http://localhost:8080/druid
        login-username: admin        # 登录账号
        login-password: 123456       # 登录密码
        allow: 127.0.0.1             # 仅本地可访问（生产环境填服务器IP）

      # ===== Web请求监控 =====
      web-stat-filter:
        enabled: true
        url-pattern: /*              # 拦截所有请求
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,/druid/*"  # 排除静态资源
```

### 4. 监控面板功能
启动后访问 `http://localhost:8080/druid`，登录后可查看：
- **数据源**：当前活跃连接、空闲连接、最大连接数
- **SQL 监控**：所有执行过的 SQL、执行次数、平均耗时
- **防火墙**：拦截到的危险 SQL 语句
- **Web 应用**：接口请求耗时、请求次数

### 5. 关键注意
- Druid 不替代 MyBatis/MP，只负责**连接管理**，SQL 仍由 MyBatis/MP 完成
- filters 三个参数：stat（SQL性能统计）、wall（SQL防火墙）、log4j2（打印日志）
- max-active 根据业务并发量设置，避免数据库连接耗尽

---

## 九、配置注入与多环境

### 1. @Value 单个读取
```yaml
# application.yml
school:
  name: 清华大学
  address: 北京
```

```java
@Value("${school.name}")    // 从配置文件读取单个属性
private String schoolName;

@Value("${school.address}")
private String address;
```

### 2. @ConfigurationProperties 批量注入（推荐）
```java
@Component
@ConfigurationProperties(prefix = "school") // 指定配置前缀，批量映射
@Data
public class SchoolConfig {
    private String name;    // 自动匹配 school.name
    private String address; // 自动匹配 school.address
}
```

| 方式 | 适用场景 |
|------|----------|
| `@Value` | 注入少量属性，简单场景 |
| `@ConfigurationProperties` | 批量注入，支持校验，企业推荐 |

### 3. 多环境配置
```
application.yml          # 主配置（指定激活哪个环境）
application-dev.yml      # 开发环境
application-prod.yml     # 生产环境
application-test.yml     # 测试环境
```

```yaml
# application.yml 主配置中指定环境
spring:
  profiles:
    active: dev   # 激活 dev 环境配置

# application-dev.yml（开发环境）
server:
  port: 8080

# application-prod.yml（生产环境）
server:
  port: 80
```

---

## 十、多模块拆分

### 父工程 pom（pom 类型，只做管理）
- `dependencyManagement`：统一锁定所有依赖版本，子模块引入时不写 `<version>`
- `<modules>`：登记全部子模块，`mvn install` 时按顺序打包
- 统一 JDK 版本、编译配置

### 四大子模块与依赖链

```
api（接口模块，可独立运行 java -jar）
  └── 依赖 service（业务逻辑）
            └── 依赖 mapper（数据库操作）
                      └── 依赖 entity（纯数据模型，仅依赖 Lombok）
```

| 模块 | 职责 | 依赖 |
|------|------|------|
| **entity** | 实体类(PO/DTO/VO)、枚举、常量 | 仅 Lombok |
| **mapper** | Mapper 接口、XML、数据源配置、分页配置 | entity + MP + Druid + MySQL |
| **service** | 业务逻辑、事务 `@Transactional`、数据组装 | mapper + entity |
| **api** | Controller、启动类、全局配置，唯一可部署模块 | service + mapper + entity + Web |

### 调用流程
前端请求 → api(Controller 接收参数) → service(执行业务逻辑、事务) → mapper(执行SQL) → entity(返回数据)

### 好处
职责单一、代码复用、解耦、按需打包、团队协作冲突少

---

## 十一、Lombok 核心注解

| 注解 | 作用 | 注意 |
|------|------|------|
| `@Data` | 一键生成 getter/setter/toString/equals/hashCode | 实体 PO 必加 |
| `@NoArgsConstructor` | 生成无参构造方法 | **不能省略**——MyBatis/MP 反射创建实体必须有无参构造 |
| `@AllArgsConstructor` | 生成全参构造方法 | 测试快速创建对象 |
| `@Builder` | 开启建造者模式，链式赋值 | `User.builder().name("张三").age(22).build()` |
| `@Slf4j` | 自动生成日志对象 `log` | Service/Controller 打日志，替代 LoggerFactory |

```java
@Data
@NoArgsConstructor  // 必须保留，MyBatis 反射需要
@AllArgsConstructor
@Builder            // 和上面两个同时加才不会冲突
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String userName;
    @TableLogic
    private Integer isDeleted;
}
```

### 注意
- IDEA 必须安装 **Lombok 插件**，否则代码红线（编译能过但编辑器报错）
- `@Builder` 必须搭配 `@NoArgsConstructor` + `@AllArgsConstructor`
