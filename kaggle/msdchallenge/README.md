# Million Song Dataset Challenge

[Kaggle competetion page](http://www.kaggle.com/c/msdchallenge)

# Solutions

## aio, 1st

For the winning solution I basically adopted a item-based collaborative filtering approach with some "crucial" modification:

1. invented a new parametric similarity between songs (or users) which lead to 0.16665 on leaderboard
2. final calibration of the scores for ranking (0.17712 on leaderboard)
3. ranking aggregation with a user-similarity based predictor (roughly 0.178 on leaderboard)

As you can see, the first two were crucial for the high scoring!

You can find a quite exaustive description of the method in this paper:

> F. Aiolli, A Preliminary Study on a Recommender System for the Million Songs Dataset Challenge Preference Learning: Problems and Applications in AI (PL-12), ECAI-12 Workshop, Montpellier
> [[http://www.ke.tu-darmstadt.de/events/PL-12/papers/08-aiolli.pdf](pdf)]

also available at my [web page](http://www.math.unipd.it/~aiolli/paperi.html)

Unfortunately, the calibration step is not fully documented and it is not discussed in the paper above.I am just preparing a new paper which describes the whole method (see the code referred below to have a rough idea of this very simple method).

I also published (a cleaned version of) the code I used for the winning submissions. It can also be used for validation. Hope it works!! There are three source files:
1. MSD_util.py, MSD utility functions
2. MSD_rec.py, MSD implementation of the basic classes: Pred (predictor) and Reco (recommender)
3. MSD_subm_rec.py, Example of a script for the computation of user recommendations

The code is not optimized and probably can be made far more efficient. I apologize for the code which is not commented appropriately. I hope it is not too criptic anyway. It might be easier to understand the code if you previously read the paper :)

I am very busy in this period and not sure I can maintain and correct the code in the future. However, I would appreciate comments and suggestions. Also, I am very courious to hear about other people' solutions..

# Ibrahim Jumkhawala, 4th
We (this is a team of 4 graduate students) tried many approaches, the one that worked best was using collaborative filtering.

For each song1 in users playlist get the song2 heard by maximum users along with song1. (get between 5-10 other songs for each song , I'm still not sure why picking up more decreases the score)

From the colisten file = > 
- song1 song2 user_count 
- song1 => in users playlist
- song2 => song with highest count of other users who heard song1 and song2
- user_count => number of users hearing song1 and song2
- song_count => number of times user hears song1,  totalCount => Number of total listens user has in playlist

give each song2 a rating
rating = (( 1+ (song_count/totalCount)) * (user_count / (total number of users who heard song1) + user_count / (total number of users who heard song2)))

Sort ratings for each song and recommend, replace leftover spaces with the most popular songs (heard by most users)

The triplet file is for one million users to give better results, song1 song2 1 have been deleted, two songs heard by only one user are removed from the file. the colisten matrix is stored on the file system in different files hashed on song1 name, so access is quick. Running the entire algo takes about 1.5 hours on a 4GB machine (4 processes).

[[Source code](https://github.com/ibby923/MSD-programs/tree/master/Final_Submission)]

## What did not work:
- Grouped artist similarity and predcited songs for similar artist, this does not work, ranks poorly.... (i still think using the whole metadata and grouping will give better results)
- Number of times user listens to song does not actally help, there is another post on kaggle saying why this data could be wrong.

## Doubts:
Not sure why using the above method and fetching more other songs per song does give better results, maybe popular song weightage beats these songs weightage.

# nhan vu, 5th

The single solution that gave me the 5rd position is based on the Adsorption algorithm from Youtube. I implemented a parallel version using multi-threading (8 threads) on a 16GB RAM, two quad core with hyper-threading. The parameters was chosen to discount popular items and greedy users who listened to many songs. I used all triplets from the taste dataset.
I reach the MAP@500 (public:0.15555/ private:0.15639) just within the first 10 days of the contest. After that, I tried many other approaches with no improvement :
- Re-rank suggestions from the Adsorption algorithm for each user based on the frequency of song years which he listened to. (0.09108/0.09081)
- Re-rank suggestions from the Adsorption algorithm for each user based on the frequency of artists whom he listened to. (0.13446/0.13420)
- Mahout ALS-WR for Implicit Feedback lambda=0.065 (0.05092/0.05099) on taste data. Just tried only 1 lambda value and give up, :(
- libFM MCMC, 100 iterations, init stdev 0.1 on taste data (0.01766/0.01721). Tried to add meta-data features (artists / tags) but it ate all memory quickly, :(
- Item similarity from Last.fm similar track db, padded with popularity-based suggestions if there is not enough suggestions (0.01167/0.01123)
- Artist and Tag item-similarity, Padded with popularity-based suggestions (0.01743/ 0.01683)

I am so surprised that:
- Matrix factorization did not help. May be I did not try different values of parameters.
- Metadata did not help too. This contest is about solving a cold-start problem and according to my knowledge, content-based algorithms do better than CF ones.
