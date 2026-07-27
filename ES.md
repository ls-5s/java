# Elasticsearch 全套学习笔记

> 项目源码路径：`src/main/java/com/example/demo/`
> 测试源码路径：`src/test/java/com/example/demo/`
> 资源路径：`src/main/resources/`

---

# 一、ES 简介（第97课）

## 1.1 是什么

Elasticsearch（ES）是基于 Lucene 开发的**分布式全文搜索引擎**，Java 开发、开源免费。

配套工具（ELK 三件套）：

| 工具 | 用途 |
|------|------|
| **Kibana** | 可视化操作台（写 DSL 代码、查数据） |
| **Logstash** | 数据采集工具 |
| **Elasticsearch** | 核心搜索引擎 |

核心用途：弥补 MySQL 模糊查询慢、不支持分词的缺陷，常用于商品搜索、文章检索、日志查询。

## 1.2 核心优势

1. **全文检索极强**：支持分词、模糊搜索、高亮、权重排序（MySQL `LIKE '%关键词%'` 海量数据下不可用）
2. **近实时查询**：数据写入约 1 秒即可搜索
3. **RESTful API**：所有操作通过 HTTP + JSON 完成
4. **易上手**：本地单节点无需复杂集群配置
5. **高扩展**：可单机学习，也可后续搭建生产集群

## 1.3 核心概念对照（ES vs MySQL）

| ES | MySQL | 说明 |
|------|------|------|
| 索引 index | 数据库 database | 数据集合容器 |
| 文档 document | 行 row | 一条数据，JSON格式 |
| 字段 field | 列 column | JSON 中的 key |
| 映射 mapping | 表结构 schema | 字段类型定义 |
| 分片 shard | 分库分表 | 数据拆分存储 |
| 副本 replica | 数据备份 | 高可用 |

> 类型 type 在 7.x 已废弃，一个索引只存一类文档。

## 1.4 应用场景

- 电商商品模糊搜索
- 博客/文章全文检索
- 项目日志检索排查 Bug
- 海量数据多条件筛选、聚合统计

## 1.5 本地学习架构

- **节点 Node**：单台 ES 服务（本地学习只用单节点）
- **主分片**：本地设 1 个即可
- **副本分片**：本地设 0（节省资源）

---

# 二、Windows 安装 & 配置（第98课）

## 2.1 版本与要求

- 首选 **ES 8.14.0 Windows 版**：自带内置 JDK，无需单独安装 Java
- Kibana 版本必须和 ES **完全一致**，否则无法连接

## 2.2 路径硬性禁忌（90% 启动失败原因）

- 解压路径**绝对不能有**：中文、空格、特殊符号
- 禁止放：桌面、中文文件夹、`Program Files`（含空格）
- 推荐：`D:\dev\elasticsearch-8.14.0`

## 2.3 目录结构

```
elasticsearch-8.14.0/
├── bin/            ← 启动脚本 elasticsearch.bat
├── config/         ← 核心配置（elasticsearch.yml、jvm.options）
├── data/           ← 数据存储
├── logs/           ← 运行日志
└── plugins/        ← IK 分词器安装目录
```

## 2.4 必改配置

### config/jvm.options — 内存配置

```
# 低配电脑防闪退、内存溢出，两个值必须一致
-Xms512m
-Xmx512m
# 4G 内存电脑可改成 256m
```

### config/elasticsearch.yml — 完整单机配置

> 冒号后必须加空格，只用空格缩进，禁止 Tab

```yaml
# 单机开发模式（Windows 必加，否则启动报错）
discovery.type: single-node

# 本地访问
network.host: 127.0.0.1
http.port: 9200

# 单分片无副本（本地学习最优）
number_of_shards: 1
number_of_replicas: 0

# 跨域支持（Kibana 需要）
http.cors.enabled: true
http.cors.allow-origin: "*"

# 关闭所有安全认证和 SSL（本地必关，否则连不上）
xpack.security.enabled: false
xpack.security.enrollment.enabled: false
xpack.security.http.ssl.enabled: false
xpack.security.transport.ssl.enabled: false
```

## 2.5 启动与验证

```bash
# 1. 管理员 CMD 进入 bin 目录
d:
cd D:\dev\elasticsearch-8.14.0\bin

# 2. 启动
elasticsearch.bat
# 出现 "started" 字样、黑窗口不闪退 = 启动成功（保持窗口打开）

# 3. 浏览器验证
http://127.0.0.1:9200
# 返回 JSON 即为正常运行
```

## 2.6 Kibana 安装配置

```bash
# 1. 同样纯英文路径解压：D:\dev\kibana-8.14.0
# 2. 修改 config/kibana.yml
```

```yaml
server.host: "127.0.0.1"
elasticsearch.hosts: ["http://127.0.0.1:9200"]
xpack.security.enabled: false
```

```bash
# 3. 启动
d:
cd D:\dev\kibana-8.14.0\bin
kibana.bat

# 4. 浏览器访问 http://127.0.0.1:5601 → Dev Tools 即可写 DSL 代码
```

## 2.7 常见报错速查

| 现象 | 原因 | 解决 |
|------|------|------|
| 双击闪退 | 路径含中文/空格、yml 格式错误 | 纯英文路径、检查 yml 冒号后空格 |
| 端口 9200 被占用 | 其他进程占用 | `netstat -ano \| findstr "9200"` 查杀进程 |
| 内存不足启动失败 | 分配内存过大 | jvm.options 改为 256m |
| 浏览器访问报 SSL 错 | 安全认证未关闭 | yml 中关闭所有 xpack 安全配置 |

---

# 三、索引操作（第99课）

## 3.1 核心知识点

| 概念 | 说明 |
|------|------|
| 索引 index | 等同于 MySQL 的 database |
| Mapping | 字段约束，等同于 MySQL 表结构 |
| `number_of_shards` | 主分片，创建后**不可修改**，单机设 1 |
| `number_of_replicas` | 副本，单机设 0 |

> 索引名规则：只能小写，不能含空格、大写、特殊符号。已有字段类型无法修改，只能新建索引 + 迁移数据。

## 3.2 字段类型速查

| 类型 | 用途 | 说明 |
|------|------|------|
| `text` | 分词模糊搜索 | 中文需配 `analyzer: "ik_max_word"` |
| `keyword` | 精确匹配、聚合、排序 | 不分词，品牌、状态、编号用这个 |
| `long` / `integer` / `double` | 数值 | 范围、排序 |
| `date` | 日期 | 需指定 `format` |

## 3.3 配套 DSL 代码（Kibana Dev Tools 执行）

### 创建索引（带 Mapping + 分片副本）

```http
PUT /goods
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "id":         { "type": "long" },
      "title":      { "type": "text", "analyzer": "ik_max_word" },
      "price":      { "type": "double" },
      "stock":      { "type": "integer" },
      "category":   { "type": "keyword" },
      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" }
    }
  }
}
```

### 查看、修改、删除索引

```http
# 查看所有索引
GET /_cat/indices?v

# 查看单个索引信息
GET /goods

# 只查看 Mapping 结构
GET /goods/_mapping

# 新增字段（已有字段不能改类型，只能新增）
PUT /goods/_mapping
{
  "properties": {
    "brand": { "type": "keyword" }
  }
}

# 删除索引（高危：数据全部清空）
DELETE /goods
```

### 索引别名

```http
# 给 goods 起别名 shop，方便多索引统一查询
POST /_aliases
{
  "actions": [
    { "add": { "index": "goods", "alias": "shop" } }
  ]
}

# 通过别名查询
GET /shop/_search
```

---

# 四、文档操作（第100课）

## 4.1 核心知识点

| 操作 | 方式 | 说明 |
|------|------|------|
| 新增（指定ID） | `PUT /index/_doc/id` | 指定 `_id` 新增/全量覆盖 |
| 新增（自动ID） | `POST /index/_doc` | ES 自动生成 `_id` |
| 查询单条 | `GET /index/_doc/id` | 根据 ID 查 |
| 全量更新 | `PUT /index/_doc/id` | 重传所有字段，**缺失字段会丢失** |
| 局部更新 | `POST /index/_doc/id/_update` | 只传要改的字段，**推荐** |
| 删除 | `DELETE /index/_doc/id` | 删除单条 |
| 批量操作 | `POST /index/_bulk` | 一次增删改多条，减少网络 IO |
| 查询全部 | `GET /index/_search` + `match_all` | 查所有文档 |
| 分页 | `from` + `size` | 分页查询 |

## 4.2 配套 DSL 代码

### 新增文档

```http
# 方式1：自定义 _id（PUT）
PUT /goods/_doc/1
{
  "id": 1,
  "title": "华为Mate70 Pro手机",
  "price": 5999.00,
  "stock": 200,
  "category": "手机数码",
  "brand": "华为",
  "createTime": "2026-07-20 10:30:00"
}

# 方式2：ES 自动生成 _id（POST，不指定 id）
POST /goods/_doc
{
  "title": "小米15 智能手机",
  "price": 3999,
  "stock": 500,
  "category": "手机数码",
  "brand": "小米"
}
```

### 查询单条

```http
GET /goods/_doc/1
```

### 修改文档

```http
# ① 全量更新：重传所有字段，缺失字段会丢失（危险）
PUT /goods/_doc/1
{
  "title": "华为Mate70 Pro 512G",
  "price": 5499,
  "stock": 150,
  "category": "手机数码",
  "brand": "华为",
  "createTime": "2026-07-20 10:30:00"
}

# ② 局部更新：只改指定字段，推荐方式
POST /goods/_doc/1/_update
{
  "doc": {
    "price": 5299,
    "stock": 120
  }
}
```

### 删除文档

```http
DELETE /goods/_doc/1
```

### 批量操作 _bulk

```http
POST /goods/_bulk
{"index":{"_id":2}}
{"title":"iPhone 16","price":6499,"category":"手机数码"}
{"update":{"_id":2}}
{"doc":{"price":6299}}
{"delete":{"_id":2}}
# 每行一个 JSON，最后必须空行
```

### 查询全部 + 分页

```http
# 查询所有
GET /goods/_search
{
  "query": {
    "match_all": {}
  }
}

# 分页查询
GET /goods/_search
{
  "from": 0,
  "size": 10,
  "query": {
    "match_all": {}
  }
}
```

---

# 五、Spring Boot 集成 — 环境搭建

## 5.1 版本说明

- ES 服务端：与 Spring Boot 版本自动适配，无需手动指定客户端版本
- 使用 Spring Data Elasticsearch 简化开发

## 5.2 pom.xml 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

## 5.3 application.yml 连接配置

```yaml
spring:
  elasticsearch:
    uris: http://127.0.0.1:9200
```

---

# 六、Spring Boot — 新增文档（两种方式）

## 6.1 方式1：Spring Data Repository（推荐新手）

### 实体类

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "goods", createIndex = true)
public class Goods {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Keyword)
    private String brand;

    private Double price;

    // === getter / setter / toString ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Override
    public String toString() {
        return "Goods{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", brand='" + brand + '\'' +
                ", price=" + price +
                '}';
    }
}
```

### Repository 持久层

```java
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
    // 内置 save() / saveAll() / findById() / findAll() 等方法，无需手写
}
```

### 测试新增

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EsAddDocTest {

    @Autowired
    private GoodsRepository goodsRepository;

    // 单条新增（id 存在则更新，不存在则新增）
    @Test
    void addSingleDoc() {
        Goods goods = new Goods();
        goods.setId(1L);
        goods.setTitle("华为Mate70 智能手机");
        goods.setBrand("华为");
        goods.setPrice(4999.0);
        goodsRepository.save(goods);
        System.out.println("单条文档添加完成");
    }

    // 批量新增
    @Test
    void addBatchDoc() {
        Goods g1 = new Goods();
        g1.setId(2L);
        g1.setTitle("小米15 手机");
        g1.setBrand("小米");
        g1.setPrice(3999.0);

        Goods g2 = new Goods();
        g2.setId(3L);
        g2.setTitle("苹果16 Pro");
        g2.setBrand("苹果");
        g2.setPrice(7999.0);

        goodsRepository.saveAll(java.util.List.of(g1, g2));
        System.out.println("批量文档添加完成");
    }
}
```

## 6.2 方式2：RestHighLevelClient 原生 API（复杂场景）

### ES 配置类

```java
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("127.0.0.1", 9200, "http"))
        );
    }
}
```

### 原生 API 新增文档

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EsClientAddTest {

    @Autowired
    private RestHighLevelClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void addDocByClient() throws Exception {
        Goods goods = new Goods();
        goods.setId(4L);
        goods.setTitle("vivo X200");
        goods.setBrand("vivo");
        goods.setPrice(3499.0);

        String json = objectMapper.writeValueAsString(goods);

        IndexRequest request = new IndexRequest("goods")
                .id("4")
                .source(json, XContentType.JSON);

        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        System.out.println("文档操作结果：" + response.result().name());
        // CREATED=新增成功  UPDATED=更新已有文档
    }
}
```

---

# 七、Spring Boot — 查询文档（两种方式）

## 7.1 核心知识点

| 概念 | 说明 |
|------|------|
| 精确查询（Keyword） | 完全匹配，不分词；品牌、状态、编号用 |
| 全文检索（Text） | IK 分词检索，标题、内容模糊搜索 |
| `Pageable` | 分页对象，**页码从 0 开始** |
| `term` | 精确匹配，只能用于 Keyword 字段 |
| `match` | 分词全文搜索，用于 Text 字段 |

> **易错点**：ES 分页 `page=0` 代表第一页，和 MySQL `page=1` 习惯不同！

## 7.2 实体类

```java
@Document(indexName = "product", createIndex = true)
public class Product {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Keyword)
    private String brand;

    private Double price;
    // getter setter toString 省略...
}
```

## 7.3 方式1：方法名自动推导（简单条件首选）

### Repository

```java
public interface ProductRepository extends ElasticsearchRepository<Product, Long> {

    // 品牌精确查询（brand 是 Keyword）
    List<Product> findByBrand(String brand);

    // 标题模糊检索（title 是 Text，分词匹配）
    List<Product> findByTitleContaining(String keyword);

    // 价格区间查询
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // 分页 + 排序
    Page<Product> findByBrand(String brand, Pageable pageable);
}
```

### 测试调用

```java
@SpringBootTest
public class EsQueryTest {

    @Autowired
    private ProductRepository repository;

    // 精确查询品牌
    @Test
    void testFindByBrand() {
        List<Product> list = repository.findByBrand("华为");
        list.forEach(System.out::println);
    }

    // 标题全文模糊搜索
    @Test
    void testSearchTitle() {
        List<Product> list = repository.findByTitleContaining("手机");
        list.forEach(System.out::println);
    }

    // 分页查询（page=0 是第一页）
    @Test
    void testPageQuery() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "price"));
        Page<Product> page = repository.findByBrand("华为", pageable);

        System.out.println("总条数：" + page.getTotalElements());
        System.out.println("数据列表：");
        page.getContent().forEach(System.out::println);
    }
}
```

## 7.4 方式2：@Query 自定义 DSL（复杂查询）

> `@Query` 里面填 ES DSL JSON，`?0` `?1` 代表第1、第2个方法参数

```java
public interface ProductRepository extends ElasticsearchRepository<Product, Long> {

    // 全文检索标题
    @Query("""
            {
              "match": {
                "title": "?0"
              }
            }
            """)
    List<Product> searchTitle(String keyword);

    // 布尔组合查询：品牌精确 + 标题模糊
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"brand": "?0"}},
                  {"match": {"title": "?1"}}
                ]
              }
            }
            """)
    List<Product> searchBrandAndTitle(String brand, String titleKeyword);
}
```

### 测试调用

```java
@Test
void testDslQuery() {
    // 全文搜索标题包含"手机"
    List<Product> list1 = repository.searchTitle("手机");

    // 品牌=华为，并且标题匹配"Mate"
    List<Product> list2 = repository.searchBrandAndTitle("华为", "Mate");
    list2.forEach(System.out::println);
}
```

## 7.5 内置查询方法（父接口自带）

```java
// 根据ID查询
Optional<Product> optional = repository.findById(1L);

// 查询全部
Iterable<Product> all = repository.findAll();

// 分页查询全部
Pageable pageable = PageRequest.of(0, 5);
Page<Product> pageAll = repository.findAll(pageable);
```

---

# 八、关键知识点总结 & 避坑

## 8.1 开发避坑清单

| # | 坑 | 正确做法 |
|---|-----|---------|
| 1 | 分页页码搞错 | ES 分页 **page=0 是第一页**，前端传 page=1 时后端要 `page-1` |
| 2 | `term` 查 Text 字段 | `term` 只能查 **Keyword** 字段，Text 字段用 `match` |
| 3 | 刚保存搜不到 | ES 默认 **1秒 refresh**，刚写入立刻查可能搜不到 |
| 4 | save 覆盖数据 | `save()` 是幂等的：id 存在则**全量覆盖**，id 不存在则新增 |
| 5 | 全量更新丢字段 | 用 `POST /_doc/id/_update` 局部更新，别用 PUT 全量覆盖 |
| 6 | 索引名字大写 | 索引名只能小写，不能含空格、大写、特殊符号 |
| 7 | 字段类型改不了 | Mapping 已创建的字段类型无法修改，只能新建索引 + 迁移数据 |

## 8.2 开发方式选择

| 场景 | 推荐方式 |
|------|---------|
| 简单 CRUD | Spring Data Repository（方法名推导） |
| 复杂 DSL / 聚合 / 批量 | `@Query` 注解 或 RestHighLevelClient |
| 高性能批量写入 | RestHighLevelClient + `_bulk` API |

## 8.3 启动顺序

1. 先启动 `elasticsearch.bat`（保持黑窗口运行）
2. 再启动 Spring Boot 项目 / 运行测试
3. 验证：浏览器访问 `http://127.0.0.1:9200/goods/_search`

## 8.4 文件速查表

| 模块 | 文件 | 说明 |
|------|------|------|
| ES 配置 | `config/elasticsearch.yml` | ES 核心配置 |
| ES 内存 | `config/jvm.options` | JVM 内存配置 |
| Kibana | `config/kibana.yml` | Kibana 连接配置 |
| Java 实体 | `Goods.java` / `Product.java` | ES 文档实体类 |
| Java 持久层 | `GoodsRepository.java` / `ProductRepository.java` | Repository 接口 |
| Java 配置 | `EsConfig.java` | RestHighLevelClient Bean |
| Java 测试 | `EsAddDocTest.java` / `EsQueryTest.java` | 单元测试 |
