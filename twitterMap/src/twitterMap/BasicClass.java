package twitterMap;
import java.util.List;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import java.util.logging.Logger;
import java.util.*;
import java.math.*;

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
	
	static class DynamoEntry {
		
		long tweetID;
		String keyWord;
		double latitude;
		double longitude;
		boolean randomData = false;
			
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
	
	public static int pushToDynamo(DynamoEntry newDynamoEntry) {
		
		DynamoClass dynamoClass = new DynamoClass();
		
		System.out.print("Tweet ID: " + newDynamoEntry.tweetID + "--");
		System.out.print("Keyword: " + newDynamoEntry.keyWord + "--");
		
		if(newDynamoEntry.randomData)
			System.out.print("Random--");
		
		System.out.println("Latitude--Longitude " + newDynamoEntry.latitude + "--" + newDynamoEntry.longitude);
		
		
		if(DynamoClass.pushRecordToDynamo(newDynamoEntry) < 0)
			return -1;
		
			
			return 0;
		
	}
	
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
		            	
		            	DynamoEntry dynamoEntry = new DynamoEntry();
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
		            	}
		            	
		            	if(pushToDynamo(dynamoEntry)<0)
		            		System.out.println("Skipped tweet. Continuing");
		            	
		            	
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
	       
  

		        
		   
	    	
	    	   
	       


	

	        
	        
	        

