# Data Mining Hackathon on BIG DATA (7GB) Best Buy mobile web site 

[[Competetion page](https://www.kaggle.com/c/acm-sf-chapter-hackathon-big)]

# Solutions

## LR, 1st

Major techniques used in my solution are summarized as following:

1. Query/keyword normalization, such as case lowering, non-alphanumeric-character removal, stopword removal, and word ordering.
2. Obtain a rule of user information: 98~99% people don't click items multiple times.
3. For head queries that have sufficient samples, the feature tuple is (query, category, week number of query time) for ML. The reason of using week number is that besbuy usually changes promotions and categories weekly
4. For tail queries that have few or no samples, kNN is used to find matched queries in a query set in which every query at least results in clicking the given category. The distance is defined  by common keyword # / max (keyword #). I believe using TF-IDF weighted distance could improve performance further. I don't optimize the algorithm of kNN since it takes less than one hour and data will be cached.

The code is written in Python. Redis is used for DB.

RAM requirement: the code makes a Redis DB of 400MB. A linux PC with 2G RAM should be able to run the code. BTW, I don't index users and queries. If do so, the DB size could be reduced by half. 

## Kingsfield, 2nd

We use naive bayes as our algorithm in this competition. The features we used is:

    query
    time
    user 

A. We want to know the probability *p(i|c)* that user click sku *i* in context *c*. We use naive bayes to predict this probability. Then select 5 item with highest predicted probability as prediction in context *c*. Here context *c* is query *q*.

The first context we used is query. By naive bayes, we have:
p(i|c) ~ p(i) * \prod p(w_k|i)

here *p(i)* is prior and we use the frequency that item *i* appears in its category as prior.
*\prod p(w_k|i)* is the likelihood and *p(w_k|i)* is the probability that word *w_k* apprears when we see item *i*.
B. We use time information in our model. We divided data into 12-day time periods based on click_time. Then used a smoothed frequency of items *i* appears in its category in its time period as prior in A.
C. We also use a bigram model to improve naive bayes. The *\prod p(w_k|i)*  likelihood in A use words conditional probability. We generate a bigram model and use naive bayes model to fit it too. We generate bigram data for each query *q* as follows:
- suppose use query “xbox call of duty”
- rerank to “call duty xbox" by alphabetic order ( with elimination of "of" as stopwords)
- bigram: [”call duty”, ”call xbox", “duty xbox“]
- use naive bayes to fit it as the same as above. 
Then we use a linear combination to ensemble the unigram prediction and bigram prediction.
D. We did some data processing. The first is query cleanning. In big version of this competition, we did 1,lemmatization;2,split words and numbers such as "xbox360" to "xbox 360". we also did some words correction in small version of this competition.
E. We rank the items that user have clicked by query *q* lower. Suppose we have prediction a,b,c,d,e when he query *q* and user *i* clicked items a,c. We rerank prediction b,d,e,a,c for user *i*.
F. We use python to implement all algorithms. The only 3rd lib need to install is nltk. We use it for lemmatization in data processing. The rest use puer python. The version of python is 2.7, as there is one function not supported in 2.6- version.  For more information, Check readme.txt in the soucecode. Please download the second attached file. Because I find nothing can delete the first attached file in the system.
