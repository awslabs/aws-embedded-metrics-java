/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package software.amazon.cloudwatchlogs.emf.util;

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

        assertEquals(str, IOUtils.toString(is));
    }

    @Test
    public void testToByteArray() throws IOException {

        String str = faker.letterify("?????");
        ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
        assertArrayEquals(str.getBytes(), IOUtils.toByteArray(is));
    }

    @Test
    public void testCloseQuitely() throws IOException {
        Logger logger = mock(Logger.class);
        InputStream is = mock(InputStream.class);
        IOUtils.closeQuietly(is, logger);

        Mockito.verify(is, times(1)).close();
    }
}
