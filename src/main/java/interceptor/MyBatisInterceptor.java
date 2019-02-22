package interceptor;

import commons.MetadataConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import query.QueryCondition;
import query.QueryParam;
import utils.SqlInjectionUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class,Integer.class}) })
public class MyBatisInterceptor implements Interceptor {

    private static final String QUERYCONDITION="queryCondition";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler statementHandler = (StatementHandler) ReflectUtil.getFieldValue(handler, "delegate");
            BoundSql boundSql = statementHandler.getBoundSql();
            Object param = boundSql.getParameterObject();
            MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(statementHandler, "mappedStatement");
            Connection connection = (Connection) invocation.getArgs()[0];
            if (param instanceof Map) {
                if (!((Map) param).containsKey(QUERYCONDITION)){
                    return invocation.proceed();
                }
                Object o = ((Map)param).get(QUERYCONDITION);
                if (null != o) {
                    String sql = sqlBuffer(boundSql.getSql(), (QueryCondition) o,connection,mappedStatement,param);
                    ReflectUtil.setFieldValue(boundSql,"sql",sql);
                }
            }
        }catch (Exception e){
            // TODO Auto-generated catch block
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler){
            return Plugin.wrap(target,this);
        }else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {

    }

    private String sqlBuffer(String sql, QueryCondition queryCondition, Connection connection, MappedStatement mappedStatement, Object param){
        StringBuffer sb=new StringBuffer(sql);
        List<QueryParam> queryParams = queryCondition.getQueryParams();
        if (queryParams!=null){
            for (QueryParam queryParam : queryParams) {
                String value=queryParam.getValue();
                if (!SqlInjectionUtils.isValid(value)){
                    // TODO Auto-generated catch block
                }
                switch (queryParam.getType()){
                    case MetadataConstants.QueryType.LIKE:
                        sb.append(" AND ").append(queryParam.getColName()).append(" LIKE '%")
                                .append(queryParam.getValue()).append("%'");
                        break;
                    case MetadataConstants.QueryType.IN:
                        String[] split = queryParam.getValue().split(",");
                        sb.append(" AND ").append(queryParam.getColName()).append(" IN('");
                        int i=0;
                        for (String s : split) {
                            i++;
                            if (split.length==i){
                                sb.append(s).append("')");
                            }else {
                                sb.append(s).append("','");
                            }
                        }
                        break;
                    case MetadataConstants.QueryType.EQUAL:
                        sb.append(" AND ").append(queryParam.getColName()).append("=")
                                .append(queryParam.getValue());
                        break;
                    case MetadataConstants.QueryType.INSET:
                        sb.append(" AND FIND_IN_SET('").append(queryParam.getValue()).append("',")
                                .append(queryParam.getColName()).append(")");
                        break;
                    case MetadataConstants.QueryType.MORETHAN:
                        sb.append(" AND ").append(queryParam.getColName()).append(">")
                                .append(queryParam.getValue());
                        break;
                    case MetadataConstants.QueryType.LESSTHAN:
                        sb.append(" AND ").append(queryParam.getColName()).append("<")
                                .append(queryParam.getValue());
                        break;
                    default:
                        break;
                }
            }
        }
        String orderBy = queryCondition.getOrderBy();
        String orderSort = queryCondition.getOrderSort();
        if (StringUtils.isNotEmpty(orderBy)){
            if (StringUtils.isEmpty(orderSort)){
                orderSort="DESC";
            }
            sb.append(" ORDER BY ").append(" ").append(orderBy).append(" ").append(orderSort);
        }
        Integer pageNum = queryCondition.getPageNum();
        Integer pageSize = queryCondition.getPageSize();
        if (pageNum!=null&&pageSize!=null){
            try {
                BoundSql boundSql = mappedStatement.getBoundSql(param);
                String countSql = getCountSql(sb.toString());
                List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
                BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), countSql, parameterMappings, param);
                ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, param, countBoundSql);
                PreparedStatement ps=connection.prepareStatement(countSql);
                parameterHandler.setParameters(ps);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()){
                    long totalCount=resultSet.getLong(1);
                    queryCondition.setTotalCount(totalCount);
                }
            }catch (Exception e){
                // TODO Auto-generated catch block
            }
            int offset=(pageNum-1)*pageSize;
            sb.append(" LIMIT ").append(offset).append(",").append(pageSize);
        }

        return sb.toString();
    }

    private String getCountSql(String sql) {

        return "select count(*) from(" + sql+")AS tmp";
    }
    private static class ReflectUtil {

        public static Object getFieldValue(Object obj, String fieldName) {
            Object result = null;
            Field field = ReflectUtil.getField(obj, fieldName);
            if (field != null) {
                field.setAccessible(true);
                try {
                    result = field.get(obj);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                }
            }
            return result;
        }

        private static Field getField(Object obj, String fieldName) {
            Field field = null;
            for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException e) {

                }
            }
            return field;
        }

        public static void setFieldValue(Object obj, String fieldName, String fieldValue) {
            Field field = ReflectUtil.getField(obj, fieldName);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    field.set(obj, fieldValue);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                }
            }
        }
    }
}
