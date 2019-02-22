package query;

import lombok.Data;

import java.util.List;

@Data
public class QueryCondition {
    private Integer pageNum;
    private Integer pageSize;
    private String orderBy; //排序字段
    private String orderSort; //正序、倒序 AES、DES
    private List<QueryParam> queryParams;
    private Long totalCount;
    private List<?> results;
    private List<String> authName;
    private String shared;
}
