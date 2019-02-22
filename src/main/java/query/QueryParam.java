package query;

import lombok.Data;

@Data
public class QueryParam {

    public QueryParam(){}

    public QueryParam(String colName, Integer type, String value){
        this.colName = colName;
        this.type = type;
        this.value = value;
    }

    private String colName;

    private Integer type;

    private String value;
}
