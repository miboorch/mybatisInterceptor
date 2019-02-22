# mybatisInterceptor
mybatis拦截器实现的针对mysql的分页排序模糊查询等功能插件


使用方法：

将插件注册到mybatis中，

在需要分页等功能的查询接口参数上加入@Param("queryCondition")QueryCondition queryCondition，
queryCondit参数示例
参数示例：QueryCondition

{

"pageNum":1,     //起始页

"pageSize":10,      //每页多少条

"orderBy":"aaaaaaa",     //根据哪一条字段排序

"orderSort":"DESC",               //正序/倒序

"queryParams":[

       {

        "colName":"bbbbbbbb",    //模糊查询字段名

        "type":1,                            //模糊查询类型 0为LIKE 1为IN 2为EQUAL 3为FIND_IN_SET（判断一个值是否在数组中）

        "value":"test"                     //模糊查询值

       }

   ]

}
