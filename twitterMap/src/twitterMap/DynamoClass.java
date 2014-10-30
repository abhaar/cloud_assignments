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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

import twitterMap.BasicClass.*;

public class DynamoClass {
	
	static AmazonDynamoDBClient dynamo;
	static String dynamoTableName = "twittMapNew";
	
	
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
	
	public static int pushRecordToDynamo(DynamoEntry newDynamoEntry){
		
		Map<String, AttributeValue> item = new HashMap <String, AttributeValue>();
		item.put("tweetID", new AttributeValue(Long.toString(newDynamoEntry.tweetID)));
		item.put("keyword", new AttributeValue(newDynamoEntry.keyWord));
		item.put("latitude", new AttributeValue(Double.toString(newDynamoEntry.latitude)));
		item.put("longitude", new AttributeValue(Double.toString(newDynamoEntry.longitude)));
		item.put("randomData", new AttributeValue(Boolean.toString(newDynamoEntry.randomData)));
		
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
		dynamoTest.initAwsSession();
	}

}
