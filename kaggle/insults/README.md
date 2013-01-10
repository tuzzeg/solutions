# Detecting Insults in Social Commentary

[Kaggle competetion page](https://www.kaggle.com/c/detecting-insults-in-social-commentary)

## Solutions:
- cbrew, 4th place [[code](https://github.com/cbrew/Insults)]
- amueller, 6th place [[code](https://github.com/amueller/kaggle_insults/)] [[blog post](http://peekaboo-vision.blogspot.de/2012/09/recap-of-my-first-kaggle-competition.html)]

## [Comments](https://www.kaggle.com/c/detecting-insults-in-social-commentary/forums/t/2744)

Vivek Sharma, 1st:
> My feature set was almost the same as the char and word features that Andreas used. SVC gave me better performance than regularized LR.  And, some normalizations (like tuzzeg mentioned), along with using a bad words list (http://urbanoalvarez.es/blog/2008/04/04/bad-words-list/) helped quite a bit. Those were probably the only differences between Andreas' score and mine. The single SVC model would have won by itself, although the winning submission combined SVC with RF which improved the score marginally over just SVC. Regularized LR and GBRT were also tried, but they did not change the score much. I did not use the datetime field.
> 
> Tuzzeg, I experimented a little bit with phrase features, and I'm pretty sure they would be needed in any implementation of such a system. A lot of the insults were of the form: "you are/you're a/an xxxx", "xxxx like you", "you xxxx". I tried to look for a large +ve/-ve word list to determine sentiment of such phrases with unseen words, but I couldn't find a good word list that was freely available for commercial use. Does anyone know of one? Ultimately, I didn't use any such features except for a very simplified one based on "you are/you're xxx" which did help the score, although, only to a small extent. 
> 
> The badwords file was taken from here: [http://urbanoalvarez.es/blog/2008/04/04/bad-words-list/](http://urbanoalvarez.es/blog/2008/04/04/bad-words-list/) and modified manually.

joshnk, 4th:
> My code is really simple, probably simpler than yours, and came fourth. I completely agree that the final test dataset was so small that there is likely an element of luck in who placed where between (at least) 3 and 10.
> 
> I pushed it to github at [https://github.com/cbrew/Insults](https://github.com/cbrew/Insults)
> 
> Comments are welcome. One generally useful thing is a version of SGD classifier that uses cross-validation and warm starts to select how many iterations.

Yasser Tabandeh, 5th:
> Congratulations to winners. I used R for this competition.
> - RWeka package was used for word tokenizing. Iterated Lovins was used as stemmer.
> - Other features which I used:
>   - Number of words in the comment
>   - Number of bad words (I used a bad word list containing 50 words)
>   - Number of phrases such as: "you are a", "you sound like"
>   - Type of date (this feature was useless in the verification set)
>   - Fisher Score was used as feature selector. 750 features were used for modeling.
> - Both decision trees and functional algorithms were used for modeling. Final model was a combination of SVM, Neural Net, random Forest, GBM, SGD, and GLMNet.

Andrei Olariu
> I also added a description of my approach (in short: SVMs, neural networks and some good tokenizing). I wanted to share this earlier, but I just couldn't find the time. 
> [http://webmining.olariu.org/my-first-kaggle-competition-and-how-i-ranked](http://webmining.olariu.org/my-first-kaggle-competition-and-how-i-ranked)

Steve Poulson, 8th
[blog post](http://steve-p0ulson.blogspot.co.uk/2012/09/recently-i-entered-kaggle-detecting.html)
