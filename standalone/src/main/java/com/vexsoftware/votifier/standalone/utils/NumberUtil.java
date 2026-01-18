package com.vexsoftware.votifier.standalone.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NumberUtil {

    public static int toInt(String str) {
        return toInt(str, 0);
    }

    public static int toInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
