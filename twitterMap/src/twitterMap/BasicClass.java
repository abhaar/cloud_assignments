package twitterMap;
import java.util.List;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.*;
import java.math.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import twitter4j.JSONException;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitterMap.DynamoClass.*;
public class BasicClass {
    //private final Logger logger = Logger.getLogger(TwitterApplication.class.getName());
	
	static AmazonSQS sqs;
	static String queueName = "TwitterQueue";
	static String queueUrl = null;
	static class DynamoEntry {
		
		long tweetID;
		String keyWord;
		double latitude;
		double longitude;
		boolean randomData = false;
		boolean sentiment = true;
		
			
	}
	
	
	static class QueueTweetEntry {
		
		long tweetID;
		String tweetText;
		double latitude;
		double longitude;
				
	}
	
	static class CoOrdinates {
		
		public double latitude;
		public double longitude;
	}
	
	public static int maxRequests = 180;
	/*Change keywords here*/
	public static List<String> keywords = Arrays.asList("ebola", "canada", "nfl", "isis", "elections");
	
	public static CoOrdinates houston;
	public static CoOrdinates sanFrancisco;
	public static CoOrdinates newYork;
	
	public static String addParantheses(String input) {
		
		String output = null;
		output = "(";
		output += input;
		output += ")";
		return output;
		
	}
	
	/*public static int pushToDynamo(DynamoEntry newDynamoEntry) {
		
		DynamoClass dynamoClass = new DynamoClass();
		
		System.out.print("Tweet ID: " + newDynamoEntry.tweetID + "--");
		System.out.print("Keyword: " + newDynamoEntry.keyWord + "--");
		
		if(newDynamoEntry.randomData)
			System.out.print("Random--");
		
		System.out.println("Latitude--Longitude " + newDynamoEntry.latitude + "--" + newDynamoEntry.longitude);
		
		
		if(DynamoClass.pushRecordToDynamo(newDynamoEntry) < 0)
			return -1;
		
			
			return 0;
		
	}*/
	
	public static CoOrdinates getRandomGeoLocation() {
		
		CoOrdinates randomCoOrdinates = new CoOrdinates();
		randomCoOrdinates.latitude = 0;
		randomCoOrdinates.longitude = 0;
		Random randomGenerator = new Random();
		
		int weight_1 = 0, weight_2 = 0, weight_3 = 0;
		randomGenerator.setSeed(System.nanoTime());
		
		while((weight_1 + weight_2 + weight_3) == 0) {
			
			weight_1 = randomGenerator.nextInt(100);
			weight_2 = randomGenerator.nextInt(100);
			weight_3 = randomGenerator.nextInt(100);
			
		}
		
		randomCoOrdinates.latitude = ((weight_1*houston.latitude)+
									(weight_2*sanFrancisco.latitude)+
									(weight_3*newYork.latitude))/
									(weight_1+weight_2+weight_3); 
		
		
		randomCoOrdinates.longitude = ((weight_1*houston.longitude)+
				(weight_2*sanFrancisco.longitude)+
				(weight_3*newYork.longitude))/
				(weight_1+weight_2+weight_3); 

		return randomCoOrdinates;
		
		
	}
	
	public static void init() {
		
		houston = new CoOrdinates();
		newYork = new CoOrdinates();
		sanFrancisco = new CoOrdinates();
		
		houston.latitude = 29.750432;
		houston.longitude = -95.361466;
		
		newYork.latitude = 40.707713;
		newYork.longitude = -74.017829;
		
		sanFrancisco.latitude = 37.773713;
		sanFrancisco.longitude = -122.408074;
		
		DynamoClass dynamoClass = new DynamoClass();
		
		if(dynamoClass.initAwsSession() < 0)
		{
			System.out.println("Error: Dynamo authentication failed");
			System.exit(-1);
		}
		
		setUpSqs();
					
	}
	
	public static int setUpSqs(){
		
		AWSCredentials credentials = null;
		
		try {
			credentials = new PropertiesCredentials(
					 BasicClass.class.getResourceAsStream("AwsCredentials.properties"));
			
						
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
		
			/*System.out.println("Sending a message to MyQueue.\n");
            sqs.sendMessage(new SendMessageRequest(queueUrl, "This is my message text2."));
      
            System.out.println("Receiving messages from MyQueue.\n");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
            List<com.amazonaws.services.sqs.model.Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            
            System.out.println(messages.size());
            
            for (com.amazonaws.services.sqs.model.Message message : messages) {
                System.out.println("  Message");
                System.out.println("    MessageId:     " + message.getMessageId());
                System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
                System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
                System.out.println("    Body:          " + message.getBody());
                for (Entry<String, String> entry : message.getAttributes().entrySet()) {
                    System.out.println("  Attribute");
                    System.out.println("    Name:  " + entry.getKey());
                    System.out.println("    Value: " + entry.getValue());
                }
            } */

		
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
	
	public static int sendTweetToSQS(String jsonTweet) {
		
		
		sqs.sendMessage(new SendMessageRequest(queueUrl, jsonTweet));
		return 0;
	}
	
	
	
	
	public static void main(String[] args) throws TwitterException {
	      
		init();		
		Twitter twitter = new TwitterFactory().getInstance();
	       String queryString = "";
	       
	       for (String ctr : keywords) {
	    	   
	    	   String paranthesizedString = (addParantheses(ctr));
	    	   queryString += paranthesizedString;
	    	   if(keywords.indexOf(ctr) != keywords.size()-1)
	    		   queryString += " OR ";
	       }
	       
	            
	       int i = 0;
	       while(1<2) {
	       
	    	   try {
		            
	    		   
	    		   Query query = new Query( queryString);
		            QueryResult result;
		            result = twitter.search(query);
		            List<Status> tweets = result.getTweets();
		            
		            
		            for (Status tweet : tweets) {
		            	
		            	
		            	QueueTweetEntry queueEntry = new QueueTweetEntry();
		            	queueEntry.tweetID = tweet.getId();
		            	queueEntry.tweetText = tweet.getText();
		            	
		            	GeoLocation generatedGeoLocation = null;
		            	generatedGeoLocation = tweet.getGeoLocation();
		            	
		            	if (generatedGeoLocation== null) {
		            		
		            		continue;
		            		/*CoOrdinates generatedCoordinates = null;
		            		generatedCoordinates = getRandomGeoLocation();
		            		queueEntry.latitude = generatedCoordinates.latitude;
		            		queueEntry.longitude = generatedCoordinates.longitude;*/
		            		//queueEntry.randomData = true;
		            		
		            	}	
		            	else {
		            	
		            		if(generatedGeoLocation.getLatitude() == 0 && generatedGeoLocation.getLongitude() == 0)
		            			continue;
		            			            		
		            		queueEntry.latitude = generatedGeoLocation.getLatitude();
		            		queueEntry.longitude = generatedGeoLocation.getLongitude();
		            		
		            		   		
		            		
		            	}
		            	
		            	
		            	JSONObject obj=new JSONObject();
		            	
		            	  try {
							obj.put("tweetID", new Long(queueEntry.tweetID));
							obj.put("tweetText",new String(queueEntry.tweetText));
			            	obj.put("latitude",new Double(queueEntry.latitude));
			            	obj.put("longitude",new Double(queueEntry.longitude));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		            	 
		            	  //System.out.println(obj.toString());  
		            	  System.out.println(obj.toString());
		            	  sendTweetToSQS(obj.toString());
		            	  
		            	  
		            	
		            	/*DynamoEntry dynamoEntry = new DynamoEntry();
		            	dynamoEntry.tweetID = tweet.getId();
		            	
		            	String tweetText = tweet.getText();
		            	if(tweetText.toLowerCase().contains(keywords.get(0).toLowerCase()))
		            		dynamoEntry.keyWord = (keywords.get(0).toLowerCase());            	
		            	else if(tweetText.toLowerCase().contains(keywords.get(1).toLowerCase()))
		            		dynamoEntry.keyWord = (keywords.get(1).toLowerCase());
		            	else if(tweetText.toLowerCase().contains(keywords.get(2).toLowerCase()))
		            		dynamoEntry.keyWord = (keywords.get(2).toLowerCase());
		            	else if(tweetText.toLowerCase().contains(keywords.get(3).toLowerCase()))
		            		dynamoEntry.keyWord = (keywords.get(3).toLowerCase());
		            	else if(tweetText.toLowerCase().contains(keywords.get(4).toLowerCase()))
		            		dynamoEntry.keyWord = (keywords.get(4).toLowerCase());
		            	else
		            		dynamoEntry.keyWord = "random";
		            	
		            	GeoLocation generatedGeoLocation = null;
		            	generatedGeoLocation = tweet.getGeoLocation();
		            	
		            	if (generatedGeoLocation== null) {
		            		
		            		CoOrdinates generatedCoordinates = null;
		            		generatedCoordinates = getRandomGeoLocation();
		            		dynamoEntry.latitude = generatedCoordinates.latitude;
			            	dynamoEntry.longitude = generatedCoordinates.longitude;
		            		dynamoEntry.randomData = true;
		            		
		            	}	
		            	else {
		            	
		            		dynamoEntry.latitude = generatedGeoLocation.getLatitude();
		            		dynamoEntry.longitude = generatedGeoLocation.getLongitude();
		            	}*/
		            	
		            	//if(pushToDynamo(dynamoEntry)<0)
		            	//	System.out.println("Skipped tweet. Continuing");
		            	
		            	
		            }
		            try {
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    	   }
	    	   
	    	     catch (TwitterException te) {
			            te.printStackTrace();
			            System.out.println("Failed to search tweets: " + te.getMessage());
			            System.exit(-1);
			        }
	    	   
	       }
	       
	}
	
}
	       
  

		        
		   
	    	
	    	   
	       


	

	        
	        
	        

