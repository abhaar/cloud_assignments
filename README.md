Assignment 1

EBS link: http://testenv-jijqphsr7i.elasticbeanstalk.com/baseFile.html

GitHub repo: https://github.com/ss91/cloud_assignments

Twitter is mined using the Search API through the twitter4j package. Using this,
tweets for keywords, defined in the code in BasicClass.java are searched for and
pushed to DynamoDB. This could be run periodically on an AWS instance, keeping
the search API limits in place and the data will be dumped in DynamoDB. I do not
have this running currently as I have used up my free tier. Hence, I have
populated a Dynamo database with a sufficient number of tweets and their
coordinates. Where, the coordinates were missing due to missing geoLocation
data, I have generated random coordinates in the mainland United States and am
using them. The TweetID and the coordinates are stored in Dynamo. The code
pertaining to this is in twitterMap/src/*.class. To test this, please add an AWS
Credentials file there. A twitter4j.properties file holding the OAuth details of
a twitter app is required in the twitterMap folder. In addition, the AWS Java
SDK libraries (1.9.3) need to be present in the classpath along with the
twitter4j libraries, which can be downloaded from:
http://twitter4j.org/en/index.html. The external JARs in the classpath file are
exhaustive and are redundant to a certain extent, but they cover all the
dependencies.

The DynamoClass creates an appropriate table, whose name is fixed.

As for plotting the tweets, /MapTwitter/webcontent/baseFile.html has the basic
required script. This essentially scans the DynamoDB and fetches the tweet
coordinates and maps them onto Google Maps, and labels them with the appropriate
keyword. There were some tweets that were mined accidentally i.e. they do not
match the preset keywords. In this case, the label is 'random'.  I had to make
several compromises for this, mainly because of my lack of familiarity with
JavaScript:

1. A random number of tweets are fetched from Dynamo every 15s and plotted on
the map - Page is refreshed every 15s. I have tried to make the tweets unique,
but to the best of my effort, I could not do this. I attempted by storing a
cookie to hold the last retrieved tweet (Dynamo Entry) and follow the scan from
there, but was unsuccessful in my attempt.Also, since I cannot afford to run an
instance to keep the tweets piling on, this made more rational sense to me.

2. In the baseFile.html, there are AWS Credentials hardcoded, because I could
not see a better way to do this. However, these are given only read-only
permissions for the DynamoDB in question.

3. Required dependencies are specified in the .classpath file but I am quite
sure it will work on its own since the JS libraries are loaded dynamically.

4. I wasn't quite sure what a screencast is supposed to do, as the link provided
by a TA seemed unavailable and hence ended up talking about the functionality
here.

5. Due to the lack of geoLocation data and a time constraint, owing to which I
decided against parsing the tweet text to actually determine an approximate
location of the tweet, I could not do any of the clustering suggestions that
were mode. I hope this is excusable, or minimally penalized.



I hope to keep the webapp running on AWS, and had to downgrade to a single
instance due to the billing. Therefore, it is my sincere request, for the
foreseeable future to not run any httperf or equivalent stress test on it, since
the single instance may not be able to handle it. However, if the cost is too
much even for the 1 instance, I might have to take it down. I am attaching a
screenshot of it working, but in case I do take it down, I will be happy to
demonstrate the functionality to you in person.

Sankalp.

