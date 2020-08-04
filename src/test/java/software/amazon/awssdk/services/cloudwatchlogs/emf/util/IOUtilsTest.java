package software.amazon.awssdk.services.cloudwatchlogs.emf.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.github.javafaker.Faker;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

public class IOUtilsTest {
    Faker faker = new Faker();

    @Test
    public void testToString() throws IOException {
        String str = faker.letterify("?????");
        ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());

        assertEquals(IOUtils.toString(is), str);
    }

    @Test
    public void testToByteArray() throws IOException {

        String str = faker.letterify("?????");
        ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
        assertArrayEquals(IOUtils.toByteArray(is), str.getBytes());
    }

    @Test
    public void testCloseQuitely() throws IOException {
        Logger logger = mock(Logger.class);
        InputStream is = mock(InputStream.class);
        IOUtils.closeQuietly(is, logger);

        Mockito.verify(is, times(1)).close();
    }
}
