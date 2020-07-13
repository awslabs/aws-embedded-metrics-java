package software.amazon.awssdk.services.cloudwatchlogs.emf.testutils;

import org.junit.Test;

import static software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities.checkThrows;

public class EMFTestUtilitiesTest {

    // Test just for coverage
    @Test
    public void constructorTest() {
        EMFTestUtilities utils = new EMFTestUtilities();
    }

    // Just make this not throw once for coverage
    @Test
    public void checkThrowsTest() {
        checkThrows(
                () -> {
                    return;
                },
                Exception.class
        );
    }
}
