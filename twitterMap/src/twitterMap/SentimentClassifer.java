package twitterMap;

import java.io.File;
import java.io.IOException;

import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.LMClassifier;
import com.aliasi.util.AbstractExternalizable;

public class SentimentClassifer {

	
	String[] categories;
	LMClassifier class_1;

	public void SentimentClassifier() {
	
	try {
		class_1= (LMClassifier) AbstractExternalizable.readObject(new File("classifier.txt"));
		categories = class_1.categories();
	}
	catch (ClassNotFoundException e) {
		e.printStackTrace();
	}
	catch (IOException e) {
		e.printStackTrace();
	}

	}

	public String classify(String text) {
	ConditionalClassification classification = class_1.classify(text);
	return classification.bestCategory();
	}
	
	
	public static void main(String args[]) {
	
		//
		
		
	}

}
