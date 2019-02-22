package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlInjectionUtils {

    private static final String REG = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
            + "(\\b(select|update|union|and|or|delete|insert|trancate|char|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";
    private static Pattern sqlPattern = Pattern.compile(REG, Pattern.CASE_INSENSITIVE);

    private static final String HIGH_RISK_REG = "(;)|(\\b(update|delete|insert|trancate|exec|into|drop|execute)\\b)";
    private static Pattern highRiskSqlPattern = Pattern.compile(HIGH_RISK_REG, Pattern.CASE_INSENSITIVE);

    public static boolean isValid(String sql)
    {
        if (sqlPattern.matcher(sql).find())
        {
            return false;
        }
        return true;
    }

    public static boolean isHighRiskOpt(String sql){

        Matcher mather = highRiskSqlPattern.matcher(sql);
        if (mather.find())
        {
            return true;
        }
        return false;
    }
}
