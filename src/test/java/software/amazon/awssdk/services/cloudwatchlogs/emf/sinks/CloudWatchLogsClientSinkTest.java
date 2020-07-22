package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DataAlreadyAcceptedException;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities.checkThrows;
import static software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities.createTinyLogItem;

public class CloudWatchLogsClientSinkTest extends CloudWatchLogsClientSinkTestBase {

    /**
     * Test for coverage
     */
    @Test
    public void testCloudWatchClientSinkConstructor() {
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();
        new CloudWatchLogsClientSink(cloudWatchLogsClient, "aLogGroup", "aLogStream");

        boolean exceptionTaken = false;
        assertTrue(checkThrows(
                () -> new CloudWatchLogsClientSink(null, "aLogGroup", "aLogStream"),
                NullPointerException.class
        ));

        assertTrue(checkThrows(
                () -> new CloudWatchLogsClientSink(cloudWatchLogsClient, null, "aLogStream"),
                NullPointerException.class
        ));

        assertTrue(checkThrows(
                () -> new CloudWatchLogsClientSink(cloudWatchLogsClient, "aLogGroup", null),
                NullPointerException.class
        ));


        CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                    .logGroup("aLogGroup")
                    .logStream("aLogStream")
                    .build(),
                NullPointerException.class
        ));


        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                    .client(cloudWatchLogsClient)
                    .logStream("aLogStream")
                    .build(),
                NullPointerException.class
        ));


        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                    .client(cloudWatchLogsClient)
                    .logGroup("aLogGroup")
                    .build(),
                NullPointerException.class
        ));


        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                        .client(null)
                        .logGroup("aLogGroup")
                        .logStream("aLogStream")
                        .build(),
                NullPointerException.class
        ));

        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                        .client(cloudWatchLogsClient)
                        .logGroup(null)
                        .logStream("aLogStream")
                        .build(),
                NullPointerException.class
        ));


        assertTrue(checkThrows(
                () -> CloudWatchLogsClientSink.builder()
                        .client(cloudWatchLogsClient)
                        .logGroup("aLogGroup")
                        .logStream(null)
                        .build(),
                NullPointerException.class
        ));


        CloudWatchLogsClientSink.builder().toString();
    }


    @Test(expected = FlushException.class)
    public void testJsonProcessingException() throws FlushException, JsonProcessingException {
        EMFLogItem li = Mockito.spy(new EMFLogItem());
        Mockito.when(li.serializeMetrics()).thenThrow(new JsonProcessingException("Oh no!") {});

        List<EMFLogItem> logItems = Arrays.asList(li);
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();
        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        sink.accept(logItems);
    }


    @Test
    public void testInvalidSequenceTokenOnce() {
        processedLogItems = new LinkedList<>();

        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();

        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .thenThrow(
                        InvalidSequenceTokenException.builder()
                                .message("Invalid Sequence Token")
                                .expectedSequenceToken("sequenceToken")
                                .build()
                )
                .then(createCloudWatchLogsClientPugLogEventsRequestSimulation());

        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = createLogItems(
                0,
                0,
                100,
                Instant.now(),
                id -> createTinyLogItem(id));

        boolean exceptionThrown = false;
        try {
            sink.accept(logItems);
        } catch (FlushException e) {
            exceptionThrown = true;
        }

        assertFalse(exceptionThrown);
    }


    @Test
    public void testInvalidSequenceTokenTwice() {
        processedLogItems = new LinkedList<>();
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();

        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .thenThrow(
                        InvalidSequenceTokenException.builder()
                                .message("Invalid Sequence Token")
                                .expectedSequenceToken("sequenceToken")
                                .build()
                );

        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = createLogItems(
                0,
                0,
                100,
                Instant.now(),
                id -> createTinyLogItem(id));

        boolean exceptionThrown = false;
        try {
            sink.accept(logItems);
        } catch (FlushException e) {
            exceptionThrown = true;
            matchLists("All items should be unprocessed", logItems, e.getUnprocessedLogItems());
            assertTrue(e.getFailedLogItems().size() == 0);
        }

        assertTrue(exceptionThrown);
    }


    /**
     * The log stream won't exist, creating the log stream will throw an exception
     * then it will work, and flushing the logs will succeed.
     */
    @Test
    public void testLogStreamDoesntExist() {
        processedLogItems = new LinkedList<>();
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();

        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .thenThrow(
                        ResourceNotFoundException.builder()
                                .message("Log Stream Doesn't Exist")
                                .build()
                )
                .thenThrow(
                        ResourceNotFoundException.builder()
                                .message("Log Stream Doesn't Exist")
                                .build()
                )
                .then(createCloudWatchLogsClientPugLogEventsRequestSimulation());

        when(cloudWatchLogsClient.createLogStream(Mockito.any(CreateLogStreamRequest.class)))
                .thenThrow(
                        SdkException.builder()
                                .message("Unexpected Exception!")
                                .build()
                )
                .then(invocation -> {
                    CreateLogStreamResponse resp = CreateLogStreamResponse.builder()
                            .build();
                    return resp;
                });

        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = createLogItems(
                0,
                0,
                100,
                Instant.now(),
                id -> createTinyLogItem(id));

        boolean exceptionThrown = false;
        try {
            sink.accept(logItems);
        } catch (FlushException e) {
            exceptionThrown = true;
        }

        assertFalse(exceptionThrown);
    }


    /**
     * The log group won't exist, creating the log group will throw an exception
     * then it will work, and flushing the logs will succeed.
     */
    @Test
    public void testLogGroupDoesntExist() {
        processedLogItems = new LinkedList<>();
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();

        // putLogEvents fails twice, then succeeds.
        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .thenThrow(
                        ResourceNotFoundException.builder()
                                .message("Log Group Doesn't Exist")
                                .build()
                )
                .thenThrow(
                        ResourceNotFoundException.builder()
                                .message("Log Group Doesn't Exist")
                                .build()
                )
                .then(createCloudWatchLogsClientPugLogEventsRequestSimulation());

        // createLogGroup fails once, then succeeds.
        when(cloudWatchLogsClient.createLogGroup(Mockito.any(CreateLogGroupRequest.class)))
                .thenThrow(
                        SdkException.builder()
                                .message("Unexpected Exception!")
                                .build()
                )
                .then(invocation -> {
                    CreateLogStreamResponse resp = CreateLogStreamResponse.builder()
                            .build();
                    return resp;
                });

        // If createLogGroup fails, then createLogStream should fail too
        when(cloudWatchLogsClient.createLogStream(Mockito.any(CreateLogStreamRequest.class)))
                .thenThrow(
                        SdkException.builder()
                                .message("Unexpected Exception!")
                                .build()
                )
                .then(invocation -> {
                    CreateLogStreamResponse resp = CreateLogStreamResponse.builder()
                            .build();
                    return resp;
                });


        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = createLogItems(
                0,
                0,
                100,
                Instant.now(),
                id -> createTinyLogItem(id));

        boolean exceptionThrown = false;
        try {
            sink.accept(logItems);
        } catch (FlushException e) {
            exceptionThrown = true;
        }

        assertFalse(exceptionThrown);
    }



    /**
     * The log group won't exist, creating the log group will throw an exception
     * then it will work, and flushing the logs will succeed.
     */
    @Test
    public void dataAlreadyAcceptedExceptionTest() {
        processedLogItems = new LinkedList<>();
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();

        // putLogEvents fails twice, then succeeds.
        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .thenThrow(
                        DataAlreadyAcceptedException.builder()
                                .message("Data already accepted")
                                .expectedSequenceToken("oi8werjnadsigfuy")
                                .build()
                )
                .then(createCloudWatchLogsClientPugLogEventsRequestSimulation());

        CloudWatchLogsClientSink sink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = createLogItems(
                0,
                0,
                100,
                Instant.now(),
                id -> createTinyLogItem(id));

        boolean exceptionThrown = false;
        try {
            sink.accept(logItems);
        } catch (FlushException e) {
            exceptionThrown = true;
        }

        assertFalse(exceptionThrown);
    }

}
