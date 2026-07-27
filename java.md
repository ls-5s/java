# Java后端速成语法（完整版｜CRUD开发专用，适配SpringBoot+MyBatis-Plus）
只保留业务高频代码，剔除冷门语法，适合速成求职，风格和你原稿统一，可直接复制保存。

## 一、基础变量与数据类型
### 1. 8大基本类型（业务常用5种）
```java
// 整数
int age = 18;
long id = 100000L;
// 小数
double price = 99.9;
// 布尔
boolean flag = true;
// 字符
char c = 'A';
```
### 2. 引用类型核心
```java
// 字符串（最常用）
String name = "张三";
```
### 3. 包装类（数据库实体强制优先，支持null）
`Integer、Long、Double、Boolean`
```java
// 数据库字段允许为空，必须用包装类，不用基础类型
private Integer age;
```

> 重点坑点：
> 基本类型默认有初始值，不能为null；包装类可以为null，极易引发空指针。

## 二、运算符、判断、循环
### 1. if / else if / else
```java
if (age >= 18) {
    System.out.println("成年");
} else if (age > 0) {
    System.out.println("未成年");
} else {
    System.out.println("非法");
}
```
### 2. 三目运算符（简单if简化）
```java
String res = age >= 18 ? "成年" : "未成年";
```
### 3. for循环
```java
List<User> list = new ArrayList<>();
// 增强for循环，日常遍历首选
for(User u : list){
    System.out.println(u.getUserName());
}
```

## 三、数组、集合（开发核心）
### 1. ArrayList 动态列表
```java
List<User> userList = new ArrayList<>();
userList.add(new User());
User u = userList.get(0);
int size = userList.size();
boolean empty = userList.isEmpty();
```
### 2. HashMap 键值对存储
```java
Map<String,Object> map = new HashMap<>();
map.put("name","李四");
String name = (String) map.get("name");
```
### 3. Stream流【重点！集合数据处理必备】
```java
// 筛选
List<User> adultList = list.stream()
        .filter(user -> user.getAge() >= 18)
        .collect(Collectors.toList());

// List转Map
Map<Long, User> userMap = list.stream()
        .collect(Collectors.toMap(User::getId, v -> v));
```

## 四、类、对象、Lombok实体（CRUD核心）
```java
package com.demo.entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String userName;
    private Integer age;
}
```
对象赋值取值
```java
User user = new User();
user.setUserName("小明");
String un = user.getUserName();
```

## 五、方法基础
格式：返回值 方法名(参数)
```java
// 有参数有返回值
public String getMsg(Integer age){
    if(age >= 18){
        return "成年";
    }
    return "未成年";
}
// 无返回值 void
public void print(){
    System.out.println("测试");
}
```
补充：Java参数传递只有**值传递**

## 六、面向对象核心（只保留业务用到）
1. **封装**：字段private，get/set（Lombok自动生成）
2. **继承 extends**
```java
public class UserServiceImpl extends ServiceImpl<UserMapper,User>
```
3. **接口 implements**
```java
public interface UserService extends IService<User>
```

### 修饰符速记
- `public`：任意地方访问
- `private`：仅本类内使用（实体字段标准用法）

### static 静态
```java
// 静态方法，无需new对象，工具类大量使用
public static boolean isEmpty(String str){
    return str == null || str.isEmpty();
}
```

## 七、枚举 Enum（业务状态必备）
订单状态、审核状态统一用枚举替代魔法数字
```java
public enum OrderStatus {
    WAIT_PAY(0,"待支付"),
    FINISH(1,"已完成");

    private Integer code;
    private String desc;
}
```

## 八、字符串高频操作 & 避坑
```java
String s = "abc";
String s2 = s + "123";
boolean has = s.contains("a");

// ✅ Spring标准判空（null/空串/全空格全部识别）
StringUtils.hasText(s);

// ⚠️ 重要坑
// == 比较内存地址，equals比较内容
"123".equals(s);
```

## 九、日期处理（现代API，项目统一规范）
优先使用 `LocalDateTime`，淘汰旧Date
```java
LocalDateTime now = LocalDateTime.now();
// 格式化
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String timeStr = now.format(formatter);
```

## 十、异常处理
```java
try{
    // 业务代码
}catch (Exception e){
    e.printStackTrace();
    // 线上不要直接打印，建议自定义异常抛出
    throw new RuntimeException("操作失败");
}
```

## 十一、Lambda（MyBatis-Plus高频）
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUserName,"张三");
```

## 十二、SpringBoot Web核心注解（写接口必备）
```java
// 实体
@Data
// Mapper层
@Mapper
// 业务层
@Service
// 接口层
@RestController
@RequestMapping("/user")
@GetMapping
@PostMapping
// 参数接收
@RequestBody     // JSON请求体
@RequestParam   // url普通参数
@PathVariable   // 路径参数
// 依赖注入
@Resource
// 事务控制
@Transactional
```

# 整体极简背诵总结
1. 基础：`Integer、String、null、equals、值传递、static`
2. 容器：`List、Map、Stream（筛选/转换）`
3. 载体：Lombok实体 + 枚举 + LocalDateTime日期
4. 逻辑：`if、for、三目运算`
5. 分层：Controller/Service/Mapper 配套注解
6. 查询：MyBatis-Plus + Lambda条件构造器
7. 风险点：空指针、字符串判空、==与equals

> 掌握这份完整版本：
> ✅ 独立开发常规CRUD业务模块
> ✅ 能处理列表转换、状态枚举、时间格式化等真实业务场景
> ✅ 覆盖初级后端面试绝大部分Java基础手写考点

如果你想要，我可以再输出一份**纯精简一页背诵版**，方便你考前快速翻看。