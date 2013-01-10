# Data Mining Hackathon on (20 mb) Best Buy mobile web site

[Kaggle page](https://www.kaggle.com/c/acm-sf-chapter-hackathon-small)

## Green Avenger, 1st
My solution mainly relies on several different string comparison techniques and by using the timestamp information (as the the overall mappings of data fluctuate over time).  I do a query comparison using the entire query string and also perform a word comparison where I split the query into words.  I create a score for each comparison, query and word, and merge them together along with some customer history to get my results. I use first the results for the entire training set and then add in changes for the particular week that the query was made.  More info below.

For my solution, I created the following tables

1. query -> sku array mapping for the entire training set time window
2. query -> sku array mapping for each day
3. word -> sku array mapping for the entire training set time window
4. word -> sku array mapping for each day
5. item -> item array mapping (if a customer bought n items within a day, those items would be associated)
6. customer queries within a day of each other

To match queries, I removed all non-alphanumeric characters including spaces.

To match words, I also removed all non-alphanumeric characters

When building the query and word tables, I first add all the entries from the product catalog (I treat the title as a query and remove the xbox 360 at the end of each title).

### Query Matching

When adding the training data, I compare each query with ones added from the product catalog and merge any misspellings within a levenshtein distance threshold. The code is conservative in its matching. If any numbers are present, each word or query being compare must have the exact same numbers (to prevent versions of games' queries being merged together).  The goal was to merge only very similar queries and words (overly aggressive matching hurt the results).  If the query doesn't merge with a query from the product catalog, a new entry is added. queries from the training set will not be merged together unless they are 100% match.

When comparing the test data queries with the tables generated from the product catalog and training data, I first look for an exact match and if found return that row of skus. Otherwise, I take the query and search for the largest word in the word table that exists in the query and then look for word matches in the remaing string of the query until I've searched the entire string.  I then take all the words found within the query, add up their sku rows into an answer and then return.  I also pull the rows from the query per day tables and use a week's worth of data (3 days before, current day, 3 days after) and apply a gaussian to weight the current day the most and farther days less.  

### Word Matching

For word matching I compare the words withing a query to the words in my word table.  If a word is not found, I use a combination of several word similarity algorithms (Levensthein, cosine, Jaccard, euclid, jarowinkler,soundex, figuring that combining their scores would smooth out each one's weaknesses) to come up wih a sim score and select the best match.  Once this is done, I add the sku rows for all the words.  I also pull the rows from the word per day tables and use a week's worth of data (3 days before, current day, 3 days after) and apply a gaussian to weight the current day the most and farther days less.  

## Adding results

I've found that combining the scores from the query matching and word matching provides a much better result than using either single one. Each method has its own strengths and weaknesses but combined works well.  I also combine data using the customer's history and similar items (though I weight this much lower than the query and word scores because it is not very reliable). In addition, I add in any other queries that were made within the last day by the customer from the test set to handle situations where a customer is looking for multiple items and doesn't necessarily buy them in the order they were queried.

### Misc

I also added a check to see if a numerical sku was entered in the query and if so make sure that the sku is part of my answer.  I also never include a sku that a customer has already purchased.

I've attached my code.  Inside is a small README.txt describing the code and how to run it.  It is written in Java.  I'll try to answer any questions. It also includes the file I submitted for my final score.

## vdaniloff, 2nd

### General model overview

The solution is based on TF-IDF model for matching queries to products. Products are indexed both from training queries and xml product data. For the test queries the products having highest tf-idf score are returned. In case of tie they are ordered by the following rules:

- The product having less number of clicks in training data is selected. Actually this was a bug, however in fact this model performed better then comparison with reverse order;
- In case of tie (this usually happens when neither of products was clicked in training set) the product having less mid-term selling rank is selected(as specified by salesRankMediumTerm tag in document). 

Both TF and IDF factors were logarithmic. Score for query was calculated as weighted sum of td-idf score of terms.
Products already viewed by customer were considered to have zero tf-idf score. If the date of query is earlier than 70 days or later than 365 days from the minimum of product start and release dates (startDate and releaseDate from xml data) then the product was considered to have zero score for this query.

### Feature extraction

The following features were extracted for indexing products in the model:

- words, forming product name (name from xml document);
- numerical sku of the product (sku from xml document);
- words, forming the training query, that led to the product;
- date of the training query, that led to the product;
- other products viewed by the same user. 

The score for features, corresponding to other product viewed by user where multiplied by discount factor of 0.8. Words, forming product name, were added to the model with term frequency of 30 (not including frequency involved by training queries).

For the test queries the following features were extracted:

- words, forming the test query;
- date of the test query;
- other products viewed by the same user in training queries. 

It can be seen that for consecutive clicks on the same query the model would output the same guess product list.

### Word extraction

The following algorithm was extracted to retrieve indexing words from the text (either query or product name):

- Hardcoded replacement ("dead rising" to "deadrising") is done with the text.
- The text is split to words using the following separator set: '  ' (space), ':' (colon), '+'( plus), '-' (minus), '/' (slash), '(' (opening round bracket), ')' (closing round bracket);
- Each of the words in the query is then simplified: dots and trailing 's are removed from the words;
- The numbers in format 20xx are changed to xx, so that 2012 is changed to 12. Idea behind is that for year-versioned games like NHL or FIFA either way refers to the same game. Note that 2k12 is not changed to 12.
- The word is possibly corrected by the spell checker (more details provided further).
- If the word is believed to be year or version (number optionally starting with 2k)  or the word concatenated with previous word appears to be a target spell checker word then it is concatenated with previous word. So the query "battle field" is changed to "battle battlefield", and the query "fifa 12" is changed to "fifa fifa12". 

### Spell checking

The spell checker is based on the following rules:

- Words shorter than 5 symbols are never corrected;
- Words with length of 5 symbols are only corrected if edit distance to correction is 0 or 1;
- Words longer than 5 symbols are corrected if edit distance to correction is less or equal to 2. 

The following edits are allowed:

- Erasing a character (any character can be erased);
- Insertion of a letter or space. 

Please note the distance used is different from Levenshtein distance. For example the following changes are  on the distance of two edits in this metrics:

- Changing a character to allowed character;
- Changing the order of two adjacent characters. 

Target words for the spell checker are provided manually based on the data analysis. Some particular corrections are also provided manually. In some cases a phrase of two words separated by the space character as target correction for the spell checker.

### Misc

Zip archive, containing code (partially javadoced) with readme file is attached. Google guava library is included to archive. Both IntelliJ IDEA project and Apache Ant build file are provided. One needs JDK 1.6+ installed to compile and run the code. Final submission file is also included to the attachment.

## Yasser Tabandeh, 3rd

Score: 0.77417

1. Divided all data into 15-day time periods based on query_time field

2. Defined a similarity measure for query strings

3. Selected top 5 SKUs ordered by:

  a. Similarity of queries

  b. Time period distance

  c. Popularity of SKU

4. Used this good observation:

"Generally, for each query, users select SKUs which didn't try before for same queries"
