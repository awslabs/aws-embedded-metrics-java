package software.amazon.awssdk.services.cloudwatchlogs.emf.util;

public class StringUtils {

    /** @return true if the given value is either null or the empty string */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
