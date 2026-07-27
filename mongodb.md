# MongoDB 全套知识点 + SpringBoot 整合（93~96课）

> 项目源码路径：`src/main/java/com/example/demo/`
> 测试源码路径：`src/test/java/com/example/demo/`
> 资源路径：`src/main/resources/`

---

# 93 MongoDB 简介

## 是什么

MongoDB 是**文档型 NoSQL 数据库**，数据以 JSON/BSON 文档存储，无固定表结构，灵活存储不规则数据。

## 和 MySQL 概念对照

| MySQL | MongoDB | 说明 |
|------|---------|------|
| 数据库 database | 数据库 database | 数据集合容器 |
| 表 table | 集合 collection | 同类文档的集合 |
| 行 row | 文档 document | 一条数据，JSON格式 |
| 列 column | 字段 field | 文档里的 key |
| 主键 id | `_id` | 文档唯一标识，自动生成 |

## 适用场景

- 字段不固定的动态数据（如安检备注、隐患图片信息）
- 日志、设备上报不规则数据
- 海量轻量数据，不需要事务、复杂联表查询

## 对比 Redis / ES

| | Redis | MongoDB | ES |
|------|------|------|------|
| 定位 | 内存缓存 | 文档持久化数据库 | 全文搜索引擎 |
| 数据 | 临时、过期 | 持久化业务数据 | 索引化文档 |
| 强项 | 高速读写 | 灵活存储不规则数据 | 全文模糊检索 |

---

# 94 MongoDB 安装

## Windows

```bash
# 1. 官网下载压缩包，解压
# 2. 创建数据目录 data/db
# 3. 启动服务
mongod --dbpath D:\mongodb\data\db

# 4. 新开 cmd 连接
mongo       # 旧版
mongosh     # 新版 shell
```

## Docker（推荐开发用）

```bash
docker run -d --name mongo -p 27017:27017 mongo:latest
```

---

# 95 基础操作命令（原生 shell）

## 库操作

```shell
# 切换/创建数据库（不存在自动创建）
use gas_check

# 查看所有库
show dbs

# 删除当前库
db.dropDatabase()
```

## 集合操作

```shell
# 创建集合
db.createCollection("check_record")

# 查看所有集合
show collections

# 删除集合
db.check_record.drop()
```

## 增（插入文档）

```shell
# 单条插入
db.check_record.insertOne({
    userId: 1001,
    name: "李四",
    mobile: "13500001234",
    address: "杭州市滨江区阳光花园",
    dangerDesc: "软管老化漏气",
    photoList: ["img1.jpg", "img2.jpg"]
})

# 批量插入多条
db.check_record.insertMany([
    { userId: 1002, name: "张三", mobile: "13600001111" },
    { userId: 1003, name: "王五", mobile: "13700002222" }
])
```

## 查（核心查询）

```shell
# 查询所有数据
db.check_record.find()
db.check_record.find().pretty()       # 格式化展示

# 条件查询
db.check_record.find({ name: "李四" })          # 精确匹配
db.check_record.find({ address: /滨江/ })       # 正则模糊匹配
db.check_record.findOne({ userId: 1001 })       # 精确匹配单条

# 分页：跳过 2 条，取 10 条
db.check_record.find().skip(2).limit(10)
```

## 修改

```shell
# 更新单条：userId=1001 → 修改手机号
db.check_record.updateOne(
    { userId: 1001 },                    # 匹配条件
    { $set: { mobile: "13999998888" } }  # 只更新指定字段
)

# 批量更新所有数据
db.check_record.updateMany(
    {},                                  # 匹配所有
    { $set: { status: 0 } }
)
```

## 删除

```shell
# 删除匹配单条
db.check_record.deleteOne({ userId: 1001 })

# 删除所有匹配数据
db.check_record.deleteMany({ status: 0 })
```

---

# 96 SpringBoot 整合 MongoDB

## 项目文件结构

```
pom.xml
src/main/resources/
└── application.yml
src/main/java/com/example/demo/
├── entity/
│   └── CheckRecord.java            ← 文档实体，映射集合
├── repository/
│   └── CheckRecordRepository.java  ← 方式一：MongoRepository 接口
├── service/
│   ├── CheckRecordService.java     ← 方式一：Repository 业务层
│   └── MongoTemplateService.java   ← 方式二：MongoTemplate 复杂查询
└── controller/
    └── CheckRecordController.java  ← REST 接口
src/test/java/com/example/demo/
└── MongoTest.java                  ← 连通性测试
```

## pom.xml

```xml
<!-- SpringBoot 整合 MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

## application.yml

```yaml
spring:
  data:
    mongodb:
      # 连接格式：mongodb://地址:端口/数据库名
      uri: mongodb://127.0.0.1:27017/gas_check
      # 有账号密码：mongodb://root:123456@127.0.0.1:27017/gas_check
```

## entity/CheckRecord.java

```java
package com.example.demo.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Id;
import java.util.List;

@Document("check_record")  // 映射到 check_record 集合
public class CheckRecord {

    @Id                       // MongoDB 自带主键 _id
    private String id;

    @Field("userId")          // 映射文档中的 userId 字段（省略时默认和属性同名）
    private Long userId;

    @Field("name")
    private String name;

    @Field("mobile")
    private String mobile;

    @Field("address")
    private String address;

    @Field("dangerDesc")
    private String dangerDesc;     // 隐患描述，字段不固定时可用 Map 替代

    @Field("photoList")
    private List<String> photoList; // 图片列表，MongoDB 天然支持数组存储

    // getter / setter 自行生成
}
```

---

## 方式一：MongoRepository（推荐快速 CRUD）

继承 `MongoRepository<T, ID>`，自动获得增删改查 + 方法名自动生成查询。

### repository/CheckRecordRepository.java

```java
package com.example.demo.repository;

import com.example.demo.entity.CheckRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CheckRecordRepository extends MongoRepository<CheckRecord, String> {

    // 方法名即查询：findBy + 字段名，Spring 自动解析实现
    List<CheckRecord> findByUserId(Long userId);          // 根据 userId 精确查询

    // Like = 模糊匹配
    List<CheckRecord> findByAddressLike(String keyword);  // 地址模糊查询
}
```

### service/CheckRecordService.java

```java
package com.example.demo.service;

import com.example.demo.entity.CheckRecord;
import com.example.demo.repository.CheckRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CheckRecordService {

    @Autowired
    private CheckRecordRepository recordRepo;

    // 新增/更新（有 id 则更新，无 id 则新增）
    public CheckRecord save(CheckRecord record) {
        return recordRepo.save(record);
    }

    // 根据主键查询
    public Optional<CheckRecord> getById(String id) {
        return recordRepo.findById(id);
    }

    // 根据用户 ID 查询所有记录
    public List<CheckRecord> getByUserId(Long userId) {
        return recordRepo.findByUserId(userId);
    }

    // 删除
    public void delete(String id) {
        recordRepo.deleteById(id);
    }

    // 查询全部
    public List<CheckRecord> listAll() {
        return recordRepo.findAll();
    }
}
```

---

## 方式二：MongoTemplate（复杂动态查询）

适合多条件组合、分页、聚合管道等复杂场景。

### service/MongoTemplateService.java

```java
package com.example.demo.service;

import com.example.demo.entity.CheckRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MongoTemplateService {

    @Autowired
    private MongoTemplate mongoTemplate; // 类似 RedisTemplate，Spring 自动装配

    // 多条件模糊分页查询
    public List<CheckRecord> searchRecord(String name, String address, int page, int size) {
        Criteria criteria = new Criteria();
        // 动态拼接条件：字段不为空才加入查询
        if (name != null && !name.isEmpty()) {
            criteria.and("name").regex(name);       // 姓名模糊匹配
        }
        if (address != null && !address.isEmpty()) {
            criteria.and("address").regex(address); // 地址模糊匹配
        }
        Query query = Query.query(criteria);
        query.with(PageRequest.of(page, size));      // 分页
        return mongoTemplate.find(query, CheckRecord.class);
    }

    // 单条插入
    public void insert(CheckRecord record) {
        mongoTemplate.insert(record);
    }
}
```

---

## controller/CheckRecordController.java

```java
package com.example.demo.controller;

import com.example.demo.entity.CheckRecord;
import com.example.demo.service.CheckRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/mongo/record")  // 统一路径前缀
public class CheckRecordController {

    @Autowired
    private CheckRecordService recordService;

    // POST /mongo/record/save  →  新增/更新安检记录
    @PostMapping("/save")
    public CheckRecord save(@RequestBody CheckRecord record) {
        return recordService.save(record);
    }

    // GET /mongo/record/list/1001  →  查询用户的所有记录
    @GetMapping("/list/{userId}")
    public List<CheckRecord> list(@PathVariable Long userId) {
        return recordService.getByUserId(userId);
    }

    // GET /mongo/record/xxx123  →  根据主键查单条
    @GetMapping("/{id}")
    public Optional<CheckRecord> get(@PathVariable String id) {
        return recordService.getById(id);
    }
}
```

---

## src/test/java/.../MongoTest.java

```java
package com.example.demo;

import com.example.demo.entity.CheckRecord;
import com.example.demo.service.CheckRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
class MongoTest {

    @Autowired
    private CheckRecordService recordService;

    // 测试新增
    @Test
    void testInsert() {
        CheckRecord record = new CheckRecord();
        record.setUserId(1001L);
        record.setName("李四");
        record.setMobile("13500001234");
        record.setAddress("杭州市滨江区阳光花园");
        record.setDangerDesc("软管老化漏气");
        record.setPhotoList(List.of("img1.jpg", "img2.jpg")); // 数组直接存入 MongoDB
        CheckRecord save = recordService.save(record);
        System.out.println("新增成功，_id：" + save.getId());  // MongoDB 自动生成的主键
    }

    // 测试查询
    @Test
    void testQuery() {
        List<CheckRecord> list = recordService.getByUserId(1001L);
        list.forEach(System.out::println);
    }
}
```

---

# 技术选型总结

| 技术 | 定位 | 典型场景 |
|------|------|---------|
| **MySQL** | 关系型核心业务 | 用户表、订单表、需要事务的强一致数据 |
| **Redis** | 内存缓存 | 热点数据加速、Session、计数器、分布式锁 |
| **MongoDB** | 文档持久化 | 动态字段业务、日志、图片元数据、安检记录 |
| **ES** | 全文搜索引擎 | 全局模糊搜索、日志检索、数据聚合统计 |

燃气安检系统推荐组合：**MySQL（主表）+ Redis（缓存）+ MongoDB（安检动态记录）+ ES（全局检索）**

---

# 文件速查表

| 章节 | 文件 | 路径 |
|------|------|------|
| 96 | `CheckRecord.java` | `src/main/java/com/example/demo/entity/` |
| 96 | `CheckRecordRepository.java` | `src/main/java/com/example/demo/repository/` |
| 96 | `CheckRecordService.java` | `src/main/java/com/example/demo/service/` |
| 96 | `MongoTemplateService.java` | `src/main/java/com/example/demo/service/` |
| 96 | `CheckRecordController.java` | `src/main/java/com/example/demo/controller/` |
| 96 | `MongoTest.java` | `src/test/java/com/example/demo/` |
| 96 | `application.yml` | `src/main/resources/` |
| 96 | `pom.xml` | 项目根目录 |
