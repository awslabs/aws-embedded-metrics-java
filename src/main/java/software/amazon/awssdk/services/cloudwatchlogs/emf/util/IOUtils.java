package software.amazon.awssdk.services.cloudwatchlogs.emf.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

public class IOUtils {
    private static final int BUFFER_SIZE = 1024 * 4;

    /**
     * Reads and returns the rest of the given input stream as a byte array. It's the caller's
     * responsibility to close the stream after the read.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[BUFFER_SIZE];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        }
    }

    /**
     * Reads and returns the rest of the given input stream as a string. It's the caller's
     * responsibility to close the stream after the read.
     */
    public static String toString(InputStream is) throws IOException {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

    /**
     * Closes the given Closeable quietly.
     *
     * @param is the given closeable
     * @param log logger used to log any failure should the close fail
     */
    public static void closeQuietly(Closeable is, Logger log) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                log.debug("Ignore failure in closing the Closeable", ex);
            }
        }
    }
}
