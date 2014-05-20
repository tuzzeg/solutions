# Facebook Recruiting III - Keyword Extraction

[Kaggle: Facebook III](http://www.kaggle.com/c/facebook-recruiting-iii-keyword-extraction)

## Comments
[Forum: Share your approach?](http://www.kaggle.com/c/facebook-recruiting-iii-keyword-extraction/forums/t/6650/share-your-approach)

### Owen (1st)

My approach:

- Take duplicates out like many others did

- Calculate the association between all 1-gram and 2-grams in title and body. I kept the first 400 chars and last 100 of body to keep the size under control. I keep only the top 30 tags for each 1-gram and 2-gram

- Predict the tags by combining all the tags associated with all the 1-gram and 2-grams in Title+body. Each 1/2 gram's impact is weighted by its support and entropy. The entropy part only improve the score very marginally.

- Take the top tags based on their score. The cut off threshold is determined by the ratio between the score of tag_k and the average score of all tags with higher scores than k.

- I scored the above on 200k posts in training, and computed the bias for each predicted tag. Some tags have more FP than FN, some have more FN than FP. The goal is to have FP and FN about equal.

- Score test data and adjust for the tag bias from step 5, i.e., if a tag has #FP>#FN, decrease its score, and vice versa.

Note:

There are quite a number of tuning parameters that I "hand optimized" by evaluating the impact on 2% of training data I kept as a validation dataset.

Title is much more important than body -- I gave a 3:1 weight to title (but this is somewhat cancelled out by the fact that there are more words in body usually)

I thought about doing some extraction but didn't get time to do so.

I also extracted quoted posts in the body and tried to match that with training, just like the duplicates. But this barely improves the result. So it seems the association of tags between posts and quoted posts are not very strong.

I also tried some TF-IDF based methods but didn't get good results.

I didn't spend too much time on "NLP" like tasks such as stemming/tagging, etc. Part of the reason is time required, and part of reason is that the posts are not very "natural", compared to texts from news archives and books. I figure I probably would lose as much information on the code part, v.s. what I can gain in the "natural language" part.

#### The "RIG":

As noted by everyone, the bottleneck of this competition is RAM. I am fortunate enough to have access to a machine with 256GB ram to try out different parameters.

The final solution would run on a desktop with 64GB of ram in about 4 hours. The training only takes about 1.5 hour and scoring will take 2.5 -- partially because it runs out of ram. Training of the model will eat up almost all of the 64GB, and the rest will swap to disk. There has to be at least 90GB swap space for this to run with out error.

I used Python, which is not very "memory efficient". If we were to compete on "efficiency" I think c/c++ would be necessary.

#### Update

On the topic of cut-off threshold of k, it is a standalone parameter I set through cross-validation. First I sort all the Tags that was positively correlated with 1/2 grams by the strength of correlation. The threshold is applied to the ratio of the correlation of Tag_k to the average correlation of Tag_1 to Tag_k-1. For each position (0-4) there is a different k.

This will determine the number of Tags to predict.

I did some very "back of the napkin" theoretical analysis on the incremental impact of adding a Tag with Prob(TP)=pk to the existing prediction with given number of TP, FP, and FN. The thresholding approach is derived from this analysis to maximize expected F1.

[Predicting Tags for StackOverflow Posts](http://iccm-conference.org/2013-proceedings/papers/0077/paper0077.pdf)

### Sergey (4th)

I approached this problem with associative rules algorithm (not sure if it’s the proper name but I never seen this approach in the books and basically it is a simplification of naive bayes approach).

- Match duplicates in test set and remove multiple duplicated entries from train set.

- Clean the data: I removed HTML tags and separated words with regular expression. Extracted all blocks with code from body. That seems important, as the code appears to be a strong indicator of programming language used and hence is good predictor for tag. Later on I added StanfordNLP parser [http://nlp.stanford.edu/software/lex-parser.shtml](http://nlp.stanford.edu/software/lex-parser.shtml) to extract lemmas. Title seems to have a bigger impact then the body so title words were extracted as separate features. Code blocks were case sensitive and processed differently, plus, I extracted some syntactic features like usage of different arrows ==> or different type of comments // or #

- Build associative rules for first 30 000 tags. I built rules on top of TF vectors, where TF(Word_j) = 1 + log(Count(Word_j)). The rule can be either positive or negative. For positive rule p(Tag_i|Word_j) > 0.3 and Sum (Word_j)>40. For negative rule p(Tag_i|Word_j) = 0 and Count(Word_j) > 50 000, where I had different weights for different type of rules and had to run multiple CV tests to select proper value. I had to run all the rules on both train and test set. Train set run was used to weight them properly. This alone seems to give ~ 0.78 and the next major improvement was introducing bigrams. I used a variation of Bloom Filter to extract candidate bigrams and had to run my algorithm in multiple stages, but that essentially pushed my score up to 0.8.

- Naïve Bayes on the first 2000 tags. I mixed results of this model into my core algorithm whenever I had 0 tags predicted.

- VowpalWabbit linear model on the first 20 tags. This marginally improved results but I decided to keep it anyway.

- The last step was to predict some additional tags based on already predicted tags. I only used predictions with high confidence. Basically I found all cases there p(Tag_i|Tag_j) is high and used them to improve predictions.

### Silogram (9th)

My approach was as follows:

- Clean the data by removing html tags and some punctuation, converting everything to lowercase and removing duplicate observations.

- Generate tfidf vectors on title and body separately (1-grams and 2-grams), and limiting the terms to 70k due to memory. Also, the tfidf vectors were based on a sample of about 50% of the non-dup observations.

- Reduced the sample to 500k observations and ran standard one-vs-rest linear regression models on the top tags (top 7000 for titles and top 3000 for bodies). Because of memory limitations, I processed the tags in groups of a couple hundred and pickled each resulting model. This produced two sets of models, one for titles and one for bodies.

- Created two additional models based on whether a tag appeared in the title or in the first 50 characters of the body. For these exact matches, it was necessary first to process the titles and bodies by replacing synonyms with their tag representation (e.g., 'win7' in a title was changed to 'windows-7'). This step produced two additional models, one for titles and bodies. In both cases, I calculated the probability that the appearance of a tag term in the title or body meant that the tag was also included in the tags field.

- In the end, I had four models, each of which gave the probability of a specific tag appearing for a specific observation. To merge these models, I removed duplicates (e.g. same tag produced by different models), ordered them in descending order of probability and selected the first 1 to 5 tags based on probability thresholds. The thresholds were determined by fitting to a validation set. The final thresholds were [0, .16, .28, .41, .46]. For example, the highest-probability tag was first selected; then the second if it's probability was over .16, then the third if its probability was over .28, etc..

My final f1 score on the non-duplicated validation set was .526.

For me, the most challenging aspect was dealing with time and memory limitations. There were a lot of factors (size of sample, number of terms in tfidf, number of tags in one-vs-rest, etc.) that affected the amount of memory and time to execute. I spent a lot of time trying to find the optimal balance of all these variables through trial-and-error. In the end, my program needed about 5 hours to generate the models and another 3 hours to generate the predictions.

### Philip Wahrlich (10th)

After some time experimenting with a range of different methods (OvA classifiers, and simple baseline models mostly), my most successful approach (0.79479) used association rules and was quite straightforward.

#### Feature Extraction

I stripped html tags, stop words (NLTK stop words), and special characters (except '+' and '#'). I kept the code blocks, as they seemed like dead giveaways for language tags. Then I extracted word unigrams, bigrams, and trigrams separately from both the title and body texts. Adding trigrams didn't give much improvement (about 0.01), and increased memory usage by quite a bit, but an improvement is an improvement. 

#### Training

I learned association rules separately from the title and body texts. This was essentially counting n-gram-tag cooccurrences in a Python dict, and keeping a count of n-gram occurrences in another Python dict. As the dictionaries grew I pruned out n-gram-tag cooccurrences which were very low in number and would be insignificant during prediction. This kept the total size of the dictionaries to about 25GB, so I could stay in RAM. I dumped the dictionaries to disk as a pickle. I trained over the whole training set (except for a small 10k sample for cross-validation).

#### Prediction

I found that the title text was far more useful than the body text, which was surprising.

I loaded the dictionaries back into RAM, then made predictions based on the title association rules. I tuned the confidences and support thresholds for 1st, 2nd, 3rd, etc predicted tags. Thresholds increased for each consecutive tag predicted. 

If there were less than 5 tags predicted from the title association rules I made further predictions based on the body association rules. Again, I tuned the confidence and support thresholds.

If no predictions were made, I forced a single prediction by removing the thresholds, and failing that, I predicted "c#".

Since some duplicates had more than one tag set associated with them in the training data, I tried to predict the most frequently occurring tag set for a given duplicate. Surprisingly, that performed worse than simply predicting the last occurring tag set, for a given duplicate, in the training set.

### elyase (14th)

irst congrats to the winners and thanks to the other participants who shared their approaches. At the beginning I also spent lots of time extracting meta-features like has_code, is_html_tag, is_email, etc. My idea was to use this "improved" representation of the documents plus all the usual bag of words features to make similarity queries to each one of the documents in the test set (bow->tf-idf->lsa). From the top most similar documents in the training set I extracted the tags and combined them to make my prediction. This made more sense to me than just blindly clustering as it would be difficult to determine the right number of clusters. Nevertheless I tried both and didn't get the results I was expecting (under 0.70 f1-score). I still think this approach has potential and needs to be further explored.

So in the last day I decided to go for a simpler approach. It was a little dramatic as I finished the code 19 hours before the submission deadline and according to my estimations the model would also take 19 hours to complete the prediction. Fortunately, the algorithm was relatively easy to parallelize and thanks to Amazon EC2 it took around 3 hours to finish the prediction in a cc2.8xlarge (wonderful machines by the way). I adapted the winning solution of the  ECML PKDD Discovery Challenge 2009 by Evangelio Milos et. al. plus some small improvements from other solutions given there [1]. The algorithm goes like this, there are three basic tag recommenders which get combined to form the final submission:

- *Basic recommender:* words from the title scored according to their "usefulness" i.e. how many times from their total appearances each word leads to a correct tag (co-occurrence). The title is a natural summarization of the post's content, which means it plays a similar role as the tags. This is a very precise recommender but has a low recall, so the set of tags has to be extended by recommenders 2 and 3.
- *Title->Tag recommender:* for each word in the title a set of tags is recommended by analyzing the corresponding nodes in a prebuilt graph Title-Tags having edges weights proportional to their co-occurrence.
- *Tag->Tag recommender:* This reproduces the fact that some tags are more probable to be found together, so I built a tag->tag graph to expand the tags from the basic recommender.

At the end I combined all tag recommendations probabilistically plus some small last minute optimizations. I'd say the main takeaway is how much statistical information is contained in the title. I intend to post the code as soon as I get to clean it a bit (it was made in a rush).

Tools used: scikits-learn (hash vectorizer, quick tests), gensim (LSA, document similarity queries), networkx(for the graphs), pypy(I didn't got any visible speed up from this, most of the time was spent accessing hash-tables within networkx).

UPDATE: Blogged about it with some code [here](http://yasermartinez.com/blog/posts/stack-exchange-tag-predictions.html).

#### Credits:
[1] http://www.kde.cs.uni-kassel.de/ws/dc09/papers/proceedings.pdf

### Damien Melksham (19th)

Software:  I don't like to enter into programming language debates, but this will come up.  In my experience, Python, and especially R, have been too slow for industrial/large scale/optimized custom-designed work.  C is not exactly a joy to code in.  I'm very good with SAS, but its specialized in what it does, and even if i wanted to use it, I don't have access to a licence via which using it for this competition wouldn't be a fire-able offense.

Subsequently, I coded the entire thing in Lisp.  Common Lisp, SBCL, using emacs and SLIME on Linux Mint to be precise.  I won't bore you with the history of Lisp.  Read up on it if you're curious.  Its one of the few languages that is relatively dynamic/high-level/low-level/compiled/REPL at the same time, but with atypical polish-prefix syntax.

If my memory serves me correctly, the libraries I used were read-csv, and cl-utilities...which probably saved a bit of time here and there.  Read-csv was used to parse the original raw data into lisp forms that could be written out in printed representation to disk.  I split the body of the post into a code section and a non-code section, based roughly upon being between the tags.  I lower-cased everything, and for the body text that wasn't deemed to be code, I roughly removed the HTML tags via an algorithm that searched for text between the '>' and '<' characters, since i considered parsing it more accurately to be outside help :P  In the end I merged to the two back together, but there's good reason to believe that this is another area for potential improvement.

The first few important functions I wrote were:

- *data accessing macro:* For exploratory work.  I coded up a form that read in each record one at a time from the main file and looped through the input text file, kind of like the SAS data step.  Each field in each record was referenced with ID, BODY, TITLE, CODE, and TAGS respectively.  In a tribute to SAS, I made a variable _N_ which was an automatic variable that kept track of the observation number irrespective of the ID.  I made some additional tools that I never really had to use.  One was an array which recorded the position of each record within the file via the array index.  A macro/function would use this to enable arbitrary random access to records within the original data file based upon their IDs, but I never really needed to use it.

- *n-gram generators:*  I wrote a character-ngram, a word-ngram, and a unique-ngram-function.  Each is capable of returning an array of arbitrary length n-grams within a text.  I only used 1-gram words in my final model, but its perfectly reasonable to assume that using more evidence would be a further improvement.  Phrases like common lisp, for instance, would presumably be picked up under word-2-grams, but lisp turns up as significant evidence for both the common-lisp tag, and the lisp tag.  Character n-grams  would allow for evidence to be gathered irrespective of arbitrary separators, and would in some way enable negation of some of my sloppy parsing/cleaning/user entry.

Now before I go on there were a number of irritating controversies which probably deserve some commentary.  When I discovered the competition I started because I believed that it was literally a competition on "tag prediction" with a test set and a training set. Two things about that...

Firstly, I observed that the scores in the leader-board just weren't theoretically possible for such a task.   An observation I stand by.  Subsequently to be sure, I programmed up simulator, which let me enter some information on number of tags, accuracy of predictions for tags and number of tags in a post, and tell me what score would be returned for such performance.  I did, and still do consider, duplicates to be against the spirit/goal of such a task, so was put in the awkward position of either calling out the scores and potentially pissing off a lot of people and possibly hurt my chances of working at facebook to find out what was going on, or keeping quiet and use it to my advantage to game the system for a high score.  I chose the former.  Mr Chester Grant soon started the next thread exposing the duplicate problem.  My point about incentives to dishonesty for such a competition, i feel, still stands.

There was one more problem, which has to do with the quality metric.  For much of the time I was working under the belief that the metric being used was the F1 measure as calculated for true positives/falsepositives/falsenegatives across all posts (which is what I interpreted as the "average F1 measure"), rather than an F1 measure taken within each post and then each F1 score averaged.  This is a subtle, but quite important difference.  One results in a totally different strategy for score maximisation compared to the other.  For one, the first gives you an incentive to not make spurious and unnecessary guesses, while the later gives you at least 1 free guess that it is optimal to take in all possible strategies.  I'm sure I'm not the only person who misunderstood this, and that such an explanation on scoring could be made clearer and more prominent.

#### Duplicate Removal: This to me was a relatively unimportant part of the competition, so I spent about a day or two coding it up, deduping the training file, and then making the links between the test and train files for fun :P

A deterministic join on parts of the title/body gets you most of them.  You can do a LITTLE bit better by going fuzzy/data linking.  As I've said earlier, this is too close to my actual work, so I didn't spend much time on this.

To fuzzily link the duplicates, I held the body of the test set in memory, and created a hash table key'd on individual words.  I already had a word count frequency loaded into another hash table, so I used the 5 least common words in each post, and pushed the ID of that post onto the hash table key'd on those words.

I then coded up a quick implementation of the Dice-sorenson coefficient, which would act as a simple distance metric between any two posts.  I read through the train set one record at a time, found the bottom 5 words in each post with the smallest frequency, and used the dice function to compare them to any of the original posts indexed by those words in the test set.  I had one more hash table, keyed on IDs in the test set, in which I stored the highest scoring dice metric and subsequent ID in the train set for each individual ID.  I arbitrarily chose a cutoff, and those ID's in the test set with a dice metric score to a record in the train set above this cutoff, i deemed duplicates.  There's lots of theory here I will neither bore you with, nor explain, since I didn't bother being too careful, but I did notice during some reviews of these links the problems with certain short common phrased questions "What's the difference between "X" and "Y"?"  Only two words different, but obviously different questions that look very similar :P

This is where it probably gets interesting, and I'm probably going to struggle to explain this without diagrams...

Lets get this out of the way.  I didn't use stop words, and I don't generally like matrices.  In many real world problems they're too inefficient.

My initial plan was to use a simple bayes formula on the individual words within title/body/code of a post.  This involved calculating the requisite probabilities of observing a given word for each tag/not-tag combination...which is not exactly a small task. So...


I am guessing everyone here is able to generate some basic summary stats.  I calculated the relevant ones and stored them in memory: number of posts, probability of each tag, frequency for each word, etc, etc.  One efficiency i noted here was that you don't need to store frequencies for words that only appear once.  If you observe a word in the train set later, and its not contained in such a frequency hash, you DEDUCE that it must be a word that is only observed once in the train set, and it is observed once only in a post with the qualities of the post in which you have currently observed it.

#### Two-deep-hashes:

I created some functions to work with double-keyed hashes.  That is to say, a hash-table whose key is a tag, and which maps each key to another hash-table that is keyed on words.

So for example, if we are training on the tags "c#" and "java" and we observe the word "c#" in a post with tags "c#" and "java", the first hash table keys are "c#" and "java", which directs us to two more hash-tables, both of these are then accessed with keys of "c#".  The values in these hashes then have their requisite counts incremented.  Upon one iteration for the tags you are training through the training set, you can then see the frequency of words for each tag.

To find out how many instances of that tag did not contain that word, and how many posts that didn't carry those tags contained that word, you iterate through the two-deep-hash-tables values and compare the requisite key/values to the original word frequencies/tag frequencies.

#### Training and Evidence

This is all well and good, but you've now got a two-deep-hash key'd on tags->words->frequencies.  What you need for your classifier to make decisions are data structures keyed on words->tags->evidence, so each time you observe a word, you know how much evidence such an observation contributes to each tag.  For this I chose a data structure that is a hash, keyed on words, mapping to an array of #(tag evidence) arrays, which can be iterated across incredibly fast.  Each time a word is observed in a post,  you hash it, which takes you to an array of evidence for requisite tags.  The function *remember-inverse-two-deep-hash* is responsible for converting information from the two-deep-hash in the training phase into that needed for the classification phase.

The functions *save-valuable-words* and *load-valuable-words* are responsible for writing the current valuable word->tag-evidence structures out to disk, and reading them in from disk respectively.

One optimization I included relatively late was the decision to change the way load-valuable-words worked to ensure that it would resolve any clashes between evidence it reads in from multiple files.  This means that you can theoretically create valuable-word structures from multiple files, allowing some form of combinations of different amounts of evidence, or allowing a later loaded file to overwrite evidence loaded from an earlier file.

Of course, this is all still relatively romantic.  The resulting structures created from these are still far too big to store all possible evidence at once, even if it doesn't have to worry about sparsity quite like a matrix.

That's where the *cull-valuable-words/cull-two-deep-hash* functions come in.  They are relatively blunt, but cut down on the amount of work a busy person like myself has to do :P

They iterate through the valuable-words/two-deep-hash structures, eliminating mappings that don't meet basic requirements of minimum probability/evidence/frequencies.  Hence I don't worry about stop words, because such words will be culled by such functions during their sweep, because by definition, they won't be contributing very much evidence to particular tags, and if they aren't eliminated, then they probably aren't stop words for that particular tag.

To classify a post, there's functions of *produce-evidence-array* and *make-simple-prediction*.  The first uses the valuable words to produce a ranked prediction of all tags for which relevant words have been observed.  The second uses the produced evidence array with a small number of parameters to make a prediction of tags for each post: say the maximum amount of evidence required to make a prediction for example.

#### Quality/Administrative Functions

There are two more aspects which might need direct commentary.  These are the very basic functions *random-sampler* and *judgement*.  The first is a simple way of iterating through the training set and holding a certain proportion of posts in memory.  The second can take predictions from the like of make-simple-prediction (or indeed any list of tags) and compare them to a truth list of tags to produce a representive F1 score.  It actually operates as several functions sharing a closure, so you can just keep feeding it records, and it increments stats as it goes, and you can ask it at any time to report how the F1 score is doing so far, check how another prediction went, or reset it.

I think that's basically it...i hope i'm not forgetting anything...

#### Improvements/lessons learnt

There's actually a fair number of improvements you can make to my model not already mentioned.  Two obvious ones i thought of include better use of the evidence-culling functions.  They are currently quite blunt, but you can change them into higher-order-functions such that their behaviors are driven more directly by the probability of observing a particular tag.  This leads me to my second improvement.  I know that my current method doesn't produce an optimal distribution of predicted tags, and optimizing this would likely result in some decent score improvements.

Although I haven't tested it, this model provides a possible claimant to the "good model with very little memory" aspect, you would just have to be much more violent with using the culling functions to make the evidence fit into memory.  Classification is a relatively memory stable operation once the valuable words have been loaded, since all that happens is that a record is read in, and its classification is written out to disk.  Of course you will do worse if you use less memory, but I'm not sure to what degree.

The classification is, relative to most of the other entries i've heard of so far, fast.  Indeed it is what I would call practically real-time.  It runs in about as fast as it takes to read the data in from disk and write out the results, although processing each uncleaned-unformatted record to establish the unique words to fit into the classification function would slow it down, i don't think we're talking hours here.

Edit: Oh, just remembered, my submitted versions effectively culled records that didn't contribute a certain amount of evidence for a tag.  I'm sure you could also add some evidence which would make certain tags less likely if a certain word is observed, but those wouldn't have been included in my model.

Edit2: Actually, there's a fair few improvements I can think of, but didn't get round to implementation.  Another one is integrating post length stats into the prediction.  My current entry basically has one classification rule across the entire set, which is obviously suboptimal.  Post length does indeed correlate with the number of tags, and the amount of evidence that could ever be collected for a post, so this is another fix if one really wanted to get that top spot :P

Edit 3: In case it wasn't clear, there were different amounts of evidence for seeing a word in the title vs seeing a word in the body of a post.

### barisumog (20th)
Seems the top ranks between private/public leaderboard haven't changed much, except of course for the top spot.

Anyways, here's a write-up of what I did. It's mostly standard stuff for text processing, but maybe it'll be helpful to some.

I used python, sklearn, and numpy/scipy. My computer is a dual core with 4GB ram. My process is broken down into about a dozen scripts, due to hardware restrictons. If it were to be run start to end, from raw data to submission file, I estimate it would take somewhere around 10-15 hours.

#### TL;DR
I cleaned the post texts, applied tf-idf, and combined it with a few meta features. With one-vs-rest approach, I trained a SGD classifier for each of the most common 10K tags. Based on the confidence scores of the classifiers on the test set, I picked the most likely tags for each post among these 10K tags.

#### Cleaning the data
My assumption was that the code fragments in the texts were too varied to be of any use, so I got rid of them all. I also stripped off the html tags, links, and urls. Also the line breaks and most punctuation, and converted all to lowercase. This left me with cleaner, conversational bits of the text. It also cut down the sizes of both train and test sets by more than half.

#### Meta features
Seeing I would now be working on less than half of the original input, I decided to incorporate some meta features from the raw text, especially about the bits I dropped off. For each post, I used:

- length of the raw text in chars
- number of code segments
- number of "a href" tags
- number of times "http" occurs (lazy way to count urls, not exact but close enough)
- number of times "greater sign" occurs (lazy way to count html tags, not exact but close enough)

I also used a couple features from the cleaned version of the text:

- number of words (tokens) in the clean text
- length of the clean text in chars

I scaled all these features to the 0-1 range, using simple min-max.
In the end, these meta features did improve the score (vs using only tf-idf on cleaned text), but not significantly.

#### Duplicates
As discussed on the forums, a lot of the posts (over a million) in the test set were already in the train set. The tags were already known, so I seperated these out as solved. In some cases (little over 100K) there were different tags given for the same question. I chose to take the union of these tags, instead of the intersection, since it scored slightly better on the leaderboard.

I also chose to prune out the duplicates from the train set. I kept the ones where same question had different results, but dropped all exact matches. My train set was now at little over 4.2 million cases.

To identify duplicates, I took the hash of clean text posts, and compared the resulting integers.

#### Tf-idf
Up until now, I was basically streaming the data through pipes, instead of loading it into memory (no pandas, I used custom generators to handle the data). But when I tried to apply tf-idf on the train set, I ran out of memory. I was still streaming the text, but my computer apparently couldn't handle all the features (there were around 5 million unique words, iirc). After various trials, I split the train set into chunks of 500K posts, and limited the number of features to 20K. My computer could handle larger numbers for the tf-idf step alone, but they got me into memory problems again in the future steps, so I limited myself at 500K x 20K.

I used the default english stopwords in sklearn. I only used single words, since including 2 or more ngrams seemed to hurt my results during the scoring stage.
And finally, I simply stacked the meta features from earlier on top of the tf-idf matrix. So my final processed input was eight sparse matrices of 500,000 x 20,007, plus the last chunk which had 206K rows. I designated the first chunk as cross validation set, and used it for testing out parameters.

#### Training and predicting
I took the one-vs-rest approach, and transformed the problem into binary classification. So I would build a model for the tag "c#", and mark the questions positive if their tags included "c#", negative otherwise. I would train seperate models this way for every tag on the input chunk, and then use the confidence scores (predict_proba or decision_function in sklearn) of the models to come up with tags.

Of course, I ran into memory problems again. So I split the process into steps, writing the results of each step to disk. I would train 1000 models, save them to disk. Then load the cv/test set, calculate confidences, save to disk. Repeat for more batches of 1000 tags. Then load the confidences for all batches of 1000 models each and combine them, and spit out tag predictions.
As the model itself, I tried Logistic Regression, Ridge Classifier, and SGD Classifier. SGD with modified huber loss gave best results, and was also the fastest, so I sticked with that.

When guessing the tags, I tried various approaches, like picking top n tags with highest confidence, or with confidence over a threshold. In the end, a mixed method seemed to work best for me. This is my predicting process, in two steps:

- from every batch of 1000 models, pick the ones with confidence over 0.20
    - if there are more than 5, pick only the top 5
    - if there are none, pick the best tag anyway
- after collecting from all batches in this way, pick the tags with confidence over 0.10
    - if there are more than 5, pick only the top 5
    - if there are none, pick the best tag anyway

It looks a bit silly, I know, but the 0.20 / 0.10 dual thresholds worked best for me in cross validation, and the scores were in line with the leaderboard. This way, I reason, rarer tags with low confidence get a chance to be picked, but only if there aren't enough frequent tags with high confidence.

#### Final submission
I ended up using the third 500K chunk for training, since it performed slightly better than others. I used only the first 10K tags (by frequency in train), thus 10K models. Beyond 10K, I started getting tags that were so rare that they weren't present in my 500K cases.

I tried setting up an ensemble of various 500K chunks, averaging the confidence scores, and predicting tags from that. But it showed very little improvement when I tried on small scale, and I was running out of time. So my final submission trains on only 500K cases of the input.

#### Things I tried that didn't seem to work
Dimensionality reduction didn't seem to work for me. I tried LSA to reduce the 20,007 features to 100-500, but scores went down. I also tried picking only the models with high confidence, but that, too, hurt the final f1 score.

I briefly played with a two tier stacking process, using the confidence scores of the models as input for a second tier of models. Again, I couldn't improve the score.

There were some tags that often come up together, like python & django, or ios & objective-c. But I wasn't able to exploit this to score better than seperate models for each tag.

#### Things I didn't try
I didn't try feature selection, I thought it would be costly to do so on each of the 10K models.

I thought of employing bagging, boosting, or just simply duplicating cases with rare tags, in order to train models better. Didn't have time to try these out.

I didn't try stemming or part of speech methods. Also, I didn't look into creating tags directly from text. So any tags in test that are not in the most frequent 10K ones in train were on my blind side.

Anyways, the worst part was of course continuously juggling data between ram and disk. I'm left with gigabytes of intermediate dumps, laying around my disk in hundreds of files.

### jerboa (42th)
I went in as anonymous, finished 42nd with 0.77091 which was fairly robust compared to my public 0.77100 last submission score.

#### Approach

sklearn, 16GB RAM on OSX, goal of making a pipeline capable of submitting in a business day.

- Create a "culled" subset of training examples that covered all 42K tags, and added random samples to skew the tag distribution back to something more representative. This was to limit the number of examples I had to use to get as much tag coverage as possible.
- Trimmed what I trained to the top 8000-11000 tags due to diminishing returns. Most of the training iterations just used 5000 tags, which I expanded later as I tuned the vectorizer and OneVsRestClassifier properly, in order to capture the last few percent.
- Trained 9 classifiers and merged their results:
- For the first two classifiers, trained on titles x 6 + bodies as the X.  Trimmed the target y to 5000-11000 tags.
- Classifier 1: TfidfVectorizer with english stopwords, l2, and a min_df designed to keep the number of features <200000.  KEY INSIGHT #1: using (1,3) on the ngrams.  Going with fewer or more ngrams degraded performance.  No tokenization or preprocessing...just used the defaults. This would typically get around 0.3940 on my internal test set for the top 5000 tags.  Threw this into OneVsRestClassifier with SGD and trained on 5000 tags.
- Classifier 2: TdidfVectorizer with english stopwords and a vocabulary set to the 42,000+ tags plus the component parts of those tags.  No ngrams.  This would get around 0.394 also until I wrote a preprocessor for it that would substitute symbols for all kinds of symbols (dollar sign, tilde, percent, brackets, etc.) as well as rolling up synonyms that the tokenizer would break apart (e.g., "windows 8" would be replaced with " symoswindows8 ", etc.  This made this classifier get up to 0.417 with OneVsRest and SGDClassifier.
- Classifier 3: A lexical approach, sort of an index learning attempt.  Break the 42000 tags into constituent parts and do a count in the titles and bodies, then predict the most common tags associated with the most common constituent parts. This could get 0.26, and largely overlapped with Classifier 2; I tried it as an experiment and the only reason I kept it was that it could at least offer options when the first two classifiers came up blank.
- Classifiers 4-8. For any training examples I could predict, I did a minibatch k-means to cluster these examples with some test examples, and created 5 clusters, with the thought that if I could train a quick and dirty classifier per cluster, I could learn something more specific than a broad cluster.  used the same approach for these 5 clusters as classifier 1.
- Classifier 9. After learning the "identity trick" on the new baseline thread, I hashed titles and created a prediction set based on identical posts/bodies.  Of course this gave me 0.58+ of the final score.
- Classifier 10. For anything the prior classifiers couldn't predict, I just used the default top 5 tags by count.

The approach then did the following after having each classifier predict against the test set:

First, if the test example was in the training set, predict the training tags.

For anything left, take the union of the output of classifiers 1 and 2, and toss out the lowest probability tag if the number of predictions was over 5.

For anything left unpredicted after that, take the prediction from the appropriate cluster that the test example belonged to.

If there still isn't a prediction after that, use the top 5 tags.

It turns out that Classifier 2 (the vocabulary approach) made Classifier 3 (the lexical approach) unnecessary.

#### Dead Ends

Before I knew what I was doing, I tried clustering the training set and then training OneVsRest classifiers for each cluster.  This was intended to make it easier to train more tags in less memory but letting me train say, 450 OvR classifiers on a small number of examples, but it got comparable to slightly inferior results with much more training time.  Ultimately, just training one or two OvR classifiers that used somewhat skewed vectorization approaches for their features produced better results in less time.

If I had to do it again, I'd go "all in" on using a vocabulary approach with TfidfVectorizer and then spend my time working on preprocessing.  Oddly, many substitutions and rollups of synonyms degraded the results.  I haven't figured out why yet.

Using SVD to reduce dimensions on raw Tfidf wasted a ton of time and didn't make a difference.  You're much better off defining your vocabulary, or using the min_df to reduce dimensions.

I had a fixation on exposing myself to as many training examples as possible--turns out that with my skewed training set, 80,000 examples (42000 culled, plus 38000 random) was often good enough, so it was much better to spend computation on more features rather than more training examples.

As others pointed out, when you train a generic model, you need a 0.024 improvement in your test set to get a 0.01 improvement in your submission score once you account for identical posts.  That makes 42nd look less impressive.  I did one test where I calculated an f1 score if I could pick the best from the predictions of the 10 classifiers (sans identical matches) and I think it was something like 0.57.  I didn't have time to figure out a good ensemble scheme or weighting approach to get closer to that theoretical max the models could produce.  Part of the problem was that sklearn and SGDClassifier require settings to get raw probabilities that might help--and these settings got poorer results than the "hinge" approach I landed on.

#### Key Insights
- If you're working with raw tokenized text, ngrams are your friend.  As is min_df
- Using a vocabulary with TfidfVectorizer is a great idea, particular with preprocessing.
- Not much gain going beyond training on the top 5000 tags. It'll move you up a few spots, but nothing earth shaking.

### Alexander D'yakonov (2nd)

tf-idf + kNN
I didn't use the hierarchy...

### nagadomi (3rd)

My approach is based on Nearest Centroid Classifier.

I didn't use the hierarchy too...

[github:nagami/kaggle-lshtc](https://github.com/nagadomi/kaggle-lshtc)

### Dmitriy Anisimov (7th)

I implemented centroid approach described in [DH Lee - Multi-Stage Rocchio Classification for Large-scale Multi-labeled Text data](http://lshtc.iit.demokritos.gr/system/files/lshc3_lee.pdf). I thought about using hierarchy and even tried one idea, but it didn't give good results and I didn't experiment with it further.

### Julian de Wit (13th)

- I wrote my own custom kNN implementation to avoid memory/scaling problems.
- Removed features from trainset that were not in testset
- 1NN on train instances with as many features as possible. I had big problems finding a good multi category selector when doing kNN so that's why I chose 1NN.
- BM25 turned out to be the best similarity function for me.
- Later I did also 1NN on the powerset categories. The features in one (powerset) category are the averaged features of all train documents  in that (powerset) category. This classifier gave me a better score. 
- Then I "ensembled" the two classifiers by just taking a union of all predicted categories. That gave me another 0.007.

In the end the solution was pretty simple. But the whole process was one big learning experience for me since I started from scratch. I'm very interested how Alexander got such good results with kNN. I thought that the higher ranked players were all mixing a number of different models/algorithms. Also tf-idf did worse for me than bm25. However.. that could have been a bug in my code.. :S
