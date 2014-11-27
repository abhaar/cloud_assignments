package twitterMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.PrintWriter;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.awt.*;

import javax.swing.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.Tables;
import com.amazonaws.services.elasticbeanstalk.model.Queue;
import com.amazonaws.services.elasticbeanstalk.model.transform.QueueStaxUnmarshaller;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.AmazonSQS.*;
import com.amazonaws.services.sqs.AmazonSQSClient.*;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import twitterMap.BasicClass.*;
import twitterMap.SentimentClassifer.*;
import twitter4j.*;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;


public class DynamoClass {
	
	
	static class DynamoEntryNew {
		
		long tweetID;
		String keyWord;
		double latitude;
		double longitude;
		String sentiment;
		
			
	}
	
	public static List<String> keywords = Arrays.asList("ebola", "canada", "nfl", "isis", "elections");
	
	static AmazonDynamoDBClient dynamo;
	static AmazonSQS sqs;
	static AmazonSNSClient snsClient;
	static String topicArn;
	static String dynamoTableName = "twitterDB";
	static String queueName = "TwitterQueue";
	static String queueUrl = null;
	
	static SentimentClassifer sentClassifier = new SentimentClassifer();
	
	public int initAwsSession (){
		
		AWSCredentials credentials = null;
		
		try {
			credentials = new PropertiesCredentials(
					 DynamoClass.class.getResourceAsStream("AwsCredentials.properties"));
			
			/*credentials = new ProfileCredentialsProvider("default").getCredentials();*/
			
			
		} catch (Exception e) {
		 	e.printStackTrace();
		 	return -1;
		}
		
		dynamo = new AmazonDynamoDBClient(credentials);
		System.out.println("Authentication Done!");
		
		if(Tables.doesTableExist(dynamo, dynamoTableName))
		{
			System.out.println("Table available");
		}
		else {
            // Create a table with a primary hash key named 'name', which holds a string
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(dynamoTableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("tweetID").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("tweetID").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(5L).withWriteCapacityUnits(5L));
                TableDescription createdTableDescription = dynamo.createTable(createTableRequest).getTableDescription();
            System.out.println("Created Table: " + createdTableDescription);

            // Wait for it to become active
            System.out.println("Waiting for " + dynamoTableName + " to become ACTIVE...");
            Tables.waitForTableToBecomeActive(dynamo, dynamoTableName);
        }
		
		return 0;
	}
	
	public static int setUpSQS(){
		
		AWSCredentials credentials = null;
		
		try {
			credentials = new PropertiesCredentials(
					 DynamoClass.class.getResourceAsStream("AwsCredentials.properties"));
			
			/*credentials = new ProfileCredentialsProvider("default").getCredentials();*/
			
			
		} catch (Exception e) {
		 	e.printStackTrace();
		 	return -1;
		}
		
		try {
			sqs = new AmazonSQSClient(credentials);
			Region usEast1 = Region.getRegion(Regions.US_EAST_1);
			sqs.setRegion(usEast1);
        
        
			CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
			queueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
			System.out.println("Queue URL: " + queueUrl);
		}
		
		catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }     
			
		return 0;
	}
	
	
	public static int initSNS() {
		
		AWSCredentials credentials = null;
		
		try {
			credentials = new PropertiesCredentials(
					 DynamoClass.class.getResourceAsStream("AwsCredentials.properties"));
			
			/*credentials = new ProfileCredentialsProvider("default").getCredentials();*/
			
			
		} catch (Exception e) {
		 	e.printStackTrace();
		 	return -1;
		}
		
		
		snsClient = new AmazonSNSClient(credentials);
		snsClient.setRegion(Region.getRegion(Regions.US_EAST_1));

		CreateTopicRequest createTopicRequest = new CreateTopicRequest("TwitterMapNotification");
		CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest);
		//print TopicArn
		System.out.println(createTopicResult);
		topicArn = createTopicResult.getTopicArn().toString();
				
		return 0;
		
		
		
	}
	
	
	
	public static int pushRecordToDynamo(DynamoEntryNew newDynamoEntry){
		
		Map<String, AttributeValue> item = new HashMap <String, AttributeValue>();
		item.put("tweetID", new AttributeValue(Long.toString(newDynamoEntry.tweetID)));
		item.put("keyword", new AttributeValue(newDynamoEntry.keyWord));
		item.put("latitude", new AttributeValue(Double.toString(newDynamoEntry.latitude)));
		item.put("longitude", new AttributeValue(Double.toString(newDynamoEntry.longitude)));
		item.put("sentiment", new AttributeValue(newDynamoEntry.sentiment));
		
		PutItemRequest putItemRequest = new PutItemRequest(dynamoTableName, item);
		PutItemResult putItemResult = dynamo.putItem(putItemRequest);
		try{
			putItemResult = dynamo.putItem(putItemRequest);
		}
		catch (Exception e){
			System.out.println("Unable to insert record");
			return -1;
		}
		
		return 0;
	}
	
	
	
	
	
	
	public static void main(String args[]) {
		
		DynamoClass dynamoTest = new DynamoClass();
		//SentimentClassifer sentClassifier = new SentimentClassifer();
		
		if(dynamoTest.initAwsSession() != 0)
			System.exit(-1);
		
		if (dynamoTest.initSNS() != 0)
			System.exit(-1);
		
		
		
		
		dynamoTest.setUpSQS();
		sentClassifier.SentimentClassifier();
		
			MonitorThread monitor = dynamoTest.new MonitorThread();
			monitor.start();
		
			ThreadB[] threadpool = new ThreadB[5];
			
			for (int i = 0; i<5; i++) {
			
				System.out.println("Starting worker thread " + i);
				
				threadpool[i] = dynamoTest.new ThreadB();
				threadpool[i].start();
						
		/*		synchronized (threadpool[i]) {
					
					try {
						threadpool[i].wait();
					} catch(InterruptedException e){
		                e.printStackTrace();
		            }
					
					
				}*/
			
			}
			
			
		while (1 < 2)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}
	
	class MonitorThread extends Thread {
		
		
		@Override
		public void run() {
			DynamoClass dynamoTest = new DynamoClass();
		
		while (1 < 2) {	
			
		int i = 0;	
		
		
		int approximateNumberOfMessages = 0;
		
		while (i < 10) {
			GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest();
			getQueueAttributesRequest.withQueueUrl(queueUrl).withAttributeNames("ApproximateNumberOfMessages");
			GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
			getQueueAttributesResult = sqs.getQueueAttributes(getQueueAttributesRequest);
			Map <String, String> attributes = getQueueAttributesResult.getAttributes();
			approximateNumberOfMessages += Integer.parseInt(attributes.get("ApproximateNumberOfMessages"));
		
			i++;
		
		}
		approximateNumberOfMessages /= 10; 
		System.out.println("Approx messages" + approximateNumberOfMessages);
		int numOfThreadsRequired = 0;
		
		numOfThreadsRequired = approximateNumberOfMessages / 500;
		
		System.out.println("Num of threads needed" + numOfThreadsRequired);
		
		if (numOfThreadsRequired < 1) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			continue;

		}
				
		ThreadB[] threadpool = new ThreadB[numOfThreadsRequired];
		
		for (i = 0; i<numOfThreadsRequired; i++) {
			
			System.out.println("Starting helper thread " + i);
			threadpool[i] = dynamoTest.new ThreadB();
			threadpool[i].start();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	}
}
	
	
	class ThreadB extends Thread{
	    int total;
	    @Override
	    public void run(){

	    	GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest();
			getQueueAttributesRequest.withQueueUrl(queueUrl).withAttributeNames("ApproximateNumberOfMessages");
						
			GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
			int mostRecentThread = 0;
			
			while(1 < 2) {
			
			getQueueAttributesResult = sqs.getQueueAttributes(getQueueAttributesRequest);
				
			int approximateNumberOfMessages = 0;
			Map <String, String> attributes = getQueueAttributesResult.getAttributes();
			
			approximateNumberOfMessages = Integer.parseInt(attributes.get("ApproximateNumberOfMessages"));	
			//System.out.println("Approximate number of messages: " + approximateNumberOfMessages);
		
			
					
			if (approximateNumberOfMessages > 0)
			{
				
				
				ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
				receiveMessageRequest.withMaxNumberOfMessages(1);
	            List<com.amazonaws.services.sqs.model.Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
	                      
	            if (messages.size() < 1)
	            	continue;
	            
	            for (com.amazonaws.services.sqs.model.Message message : messages) {
	            	
	             	 try {
						JSONObject obj = new JSONObject(message.getBody());
						String tweet = obj.getString("tweetText");
						String sent = sentClassifier.classify(tweet);
											
						DynamoEntryNew dynamoRecord = new DynamoEntryNew();
						dynamoRecord.tweetID = obj.getLong("tweetID");
							String latitude = obj.getString("latitude");
							String longitude = obj.getString("longitude");
						
						dynamoRecord.latitude = Double.parseDouble(latitude);
						dynamoRecord.longitude = Double.parseDouble(longitude);
						dynamoRecord.sentiment = sent;
						
						if(tweet.toLowerCase().contains(keywords.get(0).toLowerCase()))
		            		dynamoRecord.keyWord = (keywords.get(0).toLowerCase());            	
		            	else if(tweet.toLowerCase().contains(keywords.get(1).toLowerCase()))
		            		dynamoRecord.keyWord = (keywords.get(1).toLowerCase());
		            	else if(tweet.toLowerCase().contains(keywords.get(2).toLowerCase()))
		            		dynamoRecord.keyWord = (keywords.get(2).toLowerCase());
		            	else if(tweet.toLowerCase().contains(keywords.get(3).toLowerCase()))
		            		dynamoRecord.keyWord = (keywords.get(3).toLowerCase());
		            	else if(tweet.toLowerCase().contains(keywords.get(4).toLowerCase()))
		            		dynamoRecord.keyWord = (keywords.get(4).toLowerCase());
		            	else
		            		dynamoRecord.keyWord = "random";
						
						
						pushRecordToDynamo(dynamoRecord);
						String msg = Long.toString(dynamoRecord.tweetID);
						PublishRequest publishRequest = new PublishRequest(topicArn, msg);
						PublishResult publishResult = snsClient.publish(publishRequest);
											
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
	            	
	            }
				
	            			
			}
			
		}
	       	
	    }
	}


}
