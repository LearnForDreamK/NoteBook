#### 通用更新接口

平常某个表里的多个字段都可能经常修改，如果每个字段都专门写一个修改删除接口实在是太麻烦了，所以我借助tkmybatis（或mybatis-plus）实现了一个通用更新接口。
<!--只针对于Springboot/SpringMVC开发的接口！ 如果有网关RPC接口对外提供的服务,参数并没那么方便注入，不适合这种方式-->

tkmybatis有个很使用的通用插入/更新方法

```java
//更新对象实体,按需要修改的字段进行填值
YourClass object = new YourClass();
Example example = new Example(YourClass.class);
//这里可以对实体其他必要参数进行填充，比如时间，用户名啥的
....
//条件查询，如果需要更多的条件则继续.andEqualTo/orLike .........格式设置即可
example.createCriteria().andEqualTo("property",genzHashtagCategory.getCategoryId());
//最后使用mapper查询
mapper.updateByExampleSelective(example);
```

#### 按需添加where条件

按需添加条件

```java
Example.Criteria criteria = example.createCriteria();
//按查询条件拼接sql
if(StringUtils.isBlank(values)){
	criteria.需要的条件1().需要的条件2();
}
```

#### 按需添加大条件

```java
example.setOrderByClause("colume_name desc");
```

