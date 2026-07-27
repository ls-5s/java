# 89 Redis下载安装与基本使用 知识点+完整代码
## 一、核心知识点
### 1. Redis介绍
高性能**内存键值数据库**，读写速度远高于MySQL；常用来做缓存、分布式锁、计数器、会话存储。
### 2. 两大环境
1. Windows：本地开发调试，一键解压即用；
2. Linux：线上生产部署。
### 3. 核心组件
- redis-server：服务端程序
- redis-cli：命令行客户端
### 4. SpringBoot整合核心点
1. 引入`spring-boot-starter-data-redis`依赖；
2. yml配置地址、端口、库、密码；
3. 自动注入`StringRedisTemplate`操作缓存；
4. 五大基础数据类型：String、List、Set、Hash、ZSet。
### 5. 基础命令（客户端直接执行）
- `set key value`：存字符串
- `get key`：取字符串
- `del key`：删除key
- `hset/hget`：哈希结构
- `lpush/lrange`：列表

---
## 二、1. Windows安装步骤
1. 官网下载Redis压缩包，解压到无中文路径；
2. 打开cmd进入目录，启动服务：
```bash
redis-server.exe redis.windows.conf
```
3. 新开cmd连接客户端：
```bash
redis-cli.exe -h 127.0.0.1 -p 6379
```
4. 简单测试命令
```bash
set username 张三
get username
```

## 三、2. Linux安装（CentOS示例）
```bash
# 安装
yum install redis -y
# 启动
systemctl start redis
# 开机自启
systemctl enable redis
# 客户端连接
redis-cli
```

## 四、3. SpringBoot整合Redis完整代码
### ① Maven依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### ② application.yml 配置
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0 # 默认第0库
    password:   # 无密码留空
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
```

### ③ 测试类（基础CRUD）
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class RedisBasicTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 1. String 字符串操作
    @Test
    void testString() {
        // 存入，过期10分钟
        redisTemplate.opsForValue().set("household:name", "李四", 10, TimeUnit.MINUTES);
        // 读取
        String name = redisTemplate.opsForValue().get("household:name");
        System.out.println(name);
        // 删除
        redisTemplate.delete("household:name");
    }

    // 2. Hash 哈希（适合燃气用户多字段存储）
    @Test
    void testHash() {
        String key = "gas:user:1001";
        redisTemplate.opsForHash().put(key, "mobile", "13500001234");
        redisTemplate.opsForHash().put(key, "address", "杭州市滨江区阳光花园");
        // 单字段查询
        Object mobile = redisTemplate.opsForHash().get(key, "mobile");
        System.out.println(mobile);
    }

    // 3. List 列表
    @Test
    void testList() {
        String key = "danger:photo:list";
        redisTemplate.opsForList().leftPush(key, "图片1.jpg");
        redisTemplate.opsForList().leftPush(key, "图片2.jpg");
    }
}
```

## 五、补充说明
1. `StringRedisTemplate`：全部以字符串序列化，开发最常用；
2. 服务必须先启动`redis-server`，否则SpringBoot项目连接报错；
3. 测试环境搭配H2、业务缓存搭配Redis，分工不同：H2存测试数据库数据，Redis做高速缓存。

# 91+92 SpringBoot读写Redis客户端 知识点+完整代码
## 一、核心知识点
### 1. SpringBoot Redis客户端是什么
Spring官方封装好的Redis操作工具，底层默认Lettuce连接池，不用手写socket连接Redis服务；提供`StringRedisTemplate`（最常用）操作缓存，替代手动敲redis-cli命令。

### 2. 两个核心模板区别
1. **StringRedisTemplate（开发首选）**
    Key、Value全部序列化为字符串，可视化工具可读，适合普通缓存、字符串、哈希；
2. **RedisTemplate**
    默认Jdk序列化，存入二进制乱码，一般需要自定义序列化器，多用于存实体对象。

### 3. 核心API分组（对应Redis5大数据类型）
- opsForValue：String字符串（缓存、计数器、验证码）
- opsForHash：Hash哈希（多字段对象，如燃气用户信息）
- opsForList：List列表（有序集合、消息队列）
- opsForSet：Set无序集合（去重）
- opsForZSet：有序集合（排行榜）

### 4. 开发流程四步
1. 引入Redis依赖 + 连接池依赖
2. yml配置Redis地址、端口、连接池参数
3. 自动注入`StringRedisTemplate`
4. 调用对应ops方法完成增删改查，可设置过期时间

### 5. 连接池作用
复用Redis长连接，频繁读写时不用反复创建销毁连接，提升性能，避免连接耗尽报错。

---
## 二、完整配套代码
### 1. Maven依赖
```xml
<!-- SpringBoot Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Lettuce连接池依赖 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 2. application.yml Redis配置
```yaml
spring:
  redis:
    # Redis服务地址（本地Windows为127.0.0.1）
    host: 127.0.0.1
    port: 6379
    database: 0
    password: # 无密码留空
    lettuce:
      pool:
        max-active: 8  # 最大连接数
        max-idle: 8    # 最大空闲连接
        min-idle: 2    # 最小空闲连接
```

### 3. 单元测试类（5种数据类型实操）
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SpringBootRedisClientTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 1. String 字符串（最常用：缓存、验证码）
    @Test
    void testStringOps() {
        // 存入，过期10分钟
        redisTemplate.opsForValue().set("gas:dict:community", "阳光花园", 10, TimeUnit.MINUTES);
        // 查询
        String name = redisTemplate.opsForValue().get("gas:dict:community");
        System.out.println("小区名称：" + name);
        // 删除
        redisTemplate.delete("gas:dict:community");
    }

    // 2. Hash 哈希（存储多字段对象，用户/工单）
    @Test
    void testHashOps() {
        String userKey = "gas:user:10001";
        // 存入单个字段
        redisTemplate.opsForHash().put(userKey, "mobile", "13512345678");
        redisTemplate.opsForHash().put(userKey, "address", "滨江区阳光花园3栋");
        // 查询单个字段
        Object mobile = redisTemplate.opsForHash().get(userKey, "mobile");
        System.out.println("手机号：" + mobile);
    }

    // 3. List 列表（有序，隐患图片列表）
    @Test
    void testListOps() {
        String listKey = "danger:img:list";
        // 左侧插入
        redisTemplate.opsForList().leftPush(listKey, "img001.jpg");
        redisTemplate.opsForList().leftPush(listKey, "img002.jpg");
        // 查询全部 0~-1代表所有元素
        System.out.println(redisTemplate.opsForList().range(listKey, 0, -1));
    }

    // 4. Set 无序集合（自动去重）
    @Test
    void testSetOps() {
        String setKey = "check:user:today";
        redisTemplate.opsForSet().add(setKey, "10001", "10002", "10001");
        System.out.println(redisTemplate.opsForSet().members(setKey));
    }

    // 5. ZSet 有序集合（安检完成量排行榜）
    @Test
    void testZSetOps() {
        String rankKey = "worker:check:rank";
        // zadd key score value
        redisTemplate.opsForZSet().add(rankKey, "王师傅", 28);
        redisTemplate.opsForZSet().add(rankKey, "李师傅", 35);
        // 从小到大排序
        System.out.println(redisTemplate.opsForZSet().range(rankKey, 0, -1));
    }
}
```

### 4. 业务层简单示例（Controller使用）
```java
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RestController
public class CacheController {

    @Resource
    private StringRedisTemplate redisTemplate;

    @GetMapping("/community/get")
    public String getCommunity() {
        String key = "gas:community:001";
        // 先查缓存
        String cacheData = redisTemplate.opsForValue().get(key);
        if (cacheData != null) {
            return cacheData;
        }
        // 缓存不存在，查询数据库（模拟）
        String dbData = "滨江阳光花园小区";
        // 写入缓存，过期30分钟
        redisTemplate.opsForValue().set(key, dbData, 30, TimeUnit.MINUTES);
        return dbData;
    }
}
```

## 三、补充重点说明
1. **过期时间作用**：自动清理不常使用的缓存，避免Redis内存占满；
2. 先查缓存、无数据再查MySQL、写入缓存，这是标准**缓存查询流程**；
3. Windows本地开发必须先运行`redis-server.exe`，否则项目连接Redis会报错；
4. `StringRedisTemplate`无需额外序列化配置，开箱即用，新手优先掌握。

