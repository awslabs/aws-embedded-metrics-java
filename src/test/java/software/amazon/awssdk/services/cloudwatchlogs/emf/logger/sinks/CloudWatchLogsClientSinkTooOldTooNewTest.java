package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.function.Function;

public class CloudWatchLogsClientSinkTooOldTooNewTest extends CloudWatchLogsClientSinkTestBase {

    @Test
    public void testTooOldNormalTooNewCombinations() throws JsonProcessingException {
        final int smallNumItemsMin = 2;
        final int smallNumItemsMax = 100;

        int numTooOldItems;
        int numNormalItems;
        int numTooNewItems;

        Function<Integer, EMFLogItem> logItemSupplier = id -> EMFTestUtilities.createLargeLogItem(id);

        // Too old alone
        testTooOldNormalAndTooNew(1,
                0,
                0,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                0,
                true,
                logItemSupplier);


        // normal alone
        testTooOldNormalAndTooNew(0,
                1,
                0,
                false,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                0,
                false,
                logItemSupplier);


        // too new alone
        testTooOldNormalAndTooNew(0,
                0,
                1,
                true,
                logItemSupplier);

        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(0,
                0,
                numTooNewItems,
                true,
                logItemSupplier);


        // Too old and normal
        testTooOldNormalAndTooNew(1,
                1,
                0,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                0,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                0,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                0,
                true,
                logItemSupplier);


        // Too old and normal and too new
        testTooOldNormalAndTooNew(1,
                1,
                1,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                1,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                1,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                1,
                true,
                logItemSupplier);

        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(1,
                1,
                numTooNewItems,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);


        // normal and too new
        testTooOldNormalAndTooNew(0,
                1,
                1,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                1,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);


        // Too old  and too new
        testTooOldNormalAndTooNew(1,
                0,
                1,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                1,
                true,
                logItemSupplier);

        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(1,
                0,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(smallNumItemsMin, smallNumItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                numTooNewItems,
                true,
                logItemSupplier);
    }
}
