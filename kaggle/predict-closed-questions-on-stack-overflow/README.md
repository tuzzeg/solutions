# Predict Closed Questions on Stack Overflow

[Competetion page](https://www.kaggle.com/c/predict-closed-questions-on-stack-overflow)

[Some datases](http://meta.stackoverflow.com/questions/156571/analyzing-so-posters-experience-level)

Marco Lui, 10th

> I joined this competition fairly late in the game, partly intrigued by [Foxtrot's post](http://www.kaggle.com/c/predict-closed-questions-on-stack-overflow/forums/t/2818/beating-the-benchmark-hands-down). I'd first noticed the competition when it was announced, but had not got round to looking at it until about 1 week before the closing of the model phase. I was not sure of how to deal with the quantity of data available, as this was larger than I had tackled in the past. I had a number of sub-sampling approaches in mind, but they seemed like quite a bit of work for something that might not pay off. Foxtrot's post pointed out [Vowpal Wabbit](http://hunch.net/~vw/), which I'd previously heard of but never paid any real attention to. I saw what he was doing with it, which gave me a great platform to build from. I quickly replicated his set-up, then implemented cross-validation, then set about generating some additional features. In the end, I did better than I expected - perhaps because people made mistakes in their final submissions that they did not realize until the final scores were released, or perhaps because solutions had been tuned against the leaderboard results and ended up overfitting. I had alot of fun learning new tools, and working at a higher pace than I'm used to. In the spirit of Foxtrot's original post, I am sharing [my own implementation](https://github.com/saffsd/kaggle-stackoverflow2012).

Foxtrot, 30th [[blog post](http://fastml.com/predicting-closed-questions-on-stack-overflow/)]
