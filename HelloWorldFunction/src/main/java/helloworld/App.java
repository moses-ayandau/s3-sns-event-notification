package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * Handler for requests to Lambda function.
 * soeaoinar
 */
public class App implements RequestHandler<S3Event, String> {
    private final SnsClient snsClient;
    private final String snsTopicArn = System.getenv("SNS_TOPIC_ARN");
    private final String environment = System.getenv("ENVIRONMENT");

    public App() {
        this.snsClient = SnsClient.builder().region(Region.of(System.getenv("AWS_REGION"))).build();
    }

    public String handleRequest(S3Event event, Context context) {
        try {
            context.getLogger().log("Received S3 event: " + event);

            for (S3EventNotificationRecord record : event.getRecords()) {
                String bucketName = record.getS3().getBucket().getName();
                String objectKey = record.getS3().getObject().getKey();
                Long objectSize = record.getS3().getObject().getSizeAsLong();

                // Log the details
                context.getLogger().log("Bucket: " + bucketName);
                context.getLogger().log("Object: " + objectKey);
                context.getLogger().log("Size: " + objectSize);

                // Create notification message
                String subject = String.format("[%s] New File Upload Notification", environment.toUpperCase());
                String message = String.format(
                        "A new file has been uploaded to your S3 bucket.\n\n" +
                                "Environment: %s\n" +
                                "Bucket: %s\n" +
                                "File: %s\n" +
                                "Size: %s\n" +
                                "Time: %s",
                        environment.toUpperCase(),
                        bucketName,
                        objectKey,
                        objectSize,
                        record.getEventTime()
                );

                // Send notification
                PublishResponse result = publishToSNS(subject, message);
                context.getLogger().log("Notification sent: " + result.messageId());
            }

            return "Successfully processed " + event.getRecords().size() + " records.";
        } catch (SnsException e) {
            context.getLogger().log("Error processing S3 event: " + e.getMessage());
            throw new RuntimeException("Error processing S3 event", e);
        }
    }

    private PublishResponse publishToSNS(String subject, String message) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject(subject)
                .message(message)
                .build();

        return snsClient.publish(request);
    }
}