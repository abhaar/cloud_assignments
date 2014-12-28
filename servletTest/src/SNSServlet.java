

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.ClientConfiguration;

/**
 * Servlet implementation class SNSServlet
 */
@WebServlet("/SNSServlet")
public class SNSServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	public static int count = 0;
		
	static String queueUrl = "https://sqs.us-east-1.amazonaws.com/003617446859/TweetQueue";
    static AmazonSQS sqs;
	static AWSCredentials credentials = null;
	static int currentMessageCount = 0;
	/**
     * @throws IOException 
     * @see HttpServlet#HttpServlet()
     */
    public SNSServlet() throws IOException {
        super();
        // TODO Auto-generated constructor stub
        String accessKey = "X";
		String secretKey = "X";
		
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		sqs = new AmazonSQSClient(credentials);
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		sqs.setRegion(usEast1);
		

		        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		 response.setContentType("text/event-stream");
	       response.setCharacterEncoding("UTF-8");
		
	    PrintWriter out = response.getWriter();
		
		int approximateNumberOfMessages = 0;
		String message = null;
		GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest();
		getQueueAttributesRequest.withQueueUrl(queueUrl).withAttributeNames("ApproximateNumberOfMessages");
		GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
		getQueueAttributesResult = sqs.getQueueAttributes(getQueueAttributesRequest);
		Map <String, String> attributes = getQueueAttributesResult.getAttributes();
		approximateNumberOfMessages += Integer.parseInt(attributes.get("ApproximateNumberOfMessages"));

		if (approximateNumberOfMessages > currentMessageCount) {
			
			message = "REFRESH";
			
			out.write("data: " + message + "\n\n");
			
		}
		else {
			
			message = "STAY";
			out.write("data: " + message + "\n\n");
		
		}
		
		currentMessageCount = approximateNumberOfMessages;
	    out.write(message);
		out.close();
	    
}
		
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	

	
	
	}

}
