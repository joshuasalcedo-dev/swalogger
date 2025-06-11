package io.joshuasalcedo.logging.util;

/**
 * Utility functions for string manipulation
 */
public final class StringUtils {
    
    private StringUtils() {}
    
    // TODO: Add utility methods for string formatting, escaping, etc.
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
