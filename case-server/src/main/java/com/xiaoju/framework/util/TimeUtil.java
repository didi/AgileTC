package com.xiaoju.framework.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间类
 *
 * @author didi
 * @date 2020/11/26
 */
public class TimeUtil {

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String toString(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(pattern).format(date);
    }

    public static String toString(Date date) {
        return toString(date, DEFAULT_PATTERN);
    }

    /**
     * 时间字符串转换为Date类型
     *
     * @param timeStr 时间字符串
     * @param regex 表达式
     * @return Date类
     */
    public static Date transferStrToDateAnyRegex(String timeStr, String regex) {
        try {
            return new SimpleDateFormat(regex).parse(timeStr);
        } catch (ParseException e) {
            // 对于没有日期的用例，这里会爆出太多的error，选择不显示了
            // LOGGER.error("[日期类转换错误]str={}, regex={}, error={}", timeStr, regex, e.getMessage());
            // e.printStackTrace();
        }
        return null;
    }

    /**
     * 时间字符串转换为Date类型
     *
     * @param timeStr 时间字符串
     * @return Date类
     */
    public static Date transferStrToDateInSecond(String timeStr) {
        return transferStrToDateAnyRegex(timeStr, DEFAULT_PATTERN);
    }

    /**
     * 与原始时间1970-01-01 00:00:00 进行比较
     *
     * @param compareDate 比较日期
     * @return false 当前日期大于1970-01-01
     */
    public static boolean compareToOriginalDate(Date compareDate) {
        try {
            // 被比较日期默认为1970
            String compareTime = "1971-01-01 00:00:00";
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return compareDate(df.parse(compareTime), compareDate);
        } catch (Exception e) {
            // LOGGER.error("[日期解析出错]" );
        }
        return false;
    }

    /**
     * 比较两个日期
     *
     *
     * @param comparedDate 被比较数，相当于减法的被减数
     * @param compareDate 比较数，相当于减法中的减数
     * @return true,1号为大于等于2号位
     * false,1号位小于2号位
     */
    public static boolean compareDate(Date comparedDate, Date compareDate) {
        return comparedDate.compareTo(compareDate) >= 0;
    }

}
