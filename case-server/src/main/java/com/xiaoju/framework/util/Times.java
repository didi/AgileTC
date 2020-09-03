package com.xiaoju.framework.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Times {

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

}
