Assignment 2

EBS Link: http://testenv-jijqphsr7i.elasticbeanstalk.com/baseFile.html
GitHub repo: https://github.com/ss91/cloud_assignments


Updated to satisfy the requirements set for Assignment 2.

Tweets are mined and pushed to a SQS service from Amazon.
They are then read by a pool of threads and the sentiment
analysis and keyword analysis is done on them.
After this, the tweetID, coordinates, keyword and sentiment
is pushed to DynamoDB.

From DynamoDB the tweets are read and plotted on a Google Map
with the markers changing colors depending on the sentiment of the 
tweet.

For sentiment analysis, LingPipe API was used. 

Sankalp.

