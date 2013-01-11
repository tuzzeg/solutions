######################################################
# Run python data creation scripts

sudo ipython python/f1_setup_source_data.py | sudo tee logs/logP01.txt
# 00:15 running time

sudo ipython python/f2_dev_stim_resp.py | sudo tee logs/logP02.txt
# 00:01 running time

sudo ipython python/f3_prod_stim_resp.py | sudo tee logs/logP03.txt
# 00:01 running time

sudo ipython python/f4_period_data.py | sudo tee logs/logP04.txt
# 00:06 running time

sudo ipython python/f5_blog_rank.py | sudo tee logs/logP05.txt
# 01:10 running time

sudo ipython python/f6_topic_proximity.py | sudo tee logs/logP06.txt
# 19:45 running time

sudo ipython python/f5_blog_rank.py | sudo tee logs/logP07.txt
# 01:10 running time, run again due to new 'candidates' from f6

sudo ipython python/f7_language.py | sudo tee logs/logP08.txt
# 02:45 running time

sudo ipython python/f8_timezone.py | sudo tee logs/logP09.txt
# 00:30 running time

sudo ipython python/f9_author_stats.py | sudo tee logs/logP10.txt
# 00:30 running time

sudo ipython python/assemble_model_set.py | sudo tee logs/logP11.txt
# 02:15 running time

sudo ipython python/assemble_dev_actuals.py | sudo tee logs/logP12.txt
# 00:01 running time


######################################################
# Run r submission creation scripts

sudo R -f "r/models.r" | sudo tee logs/logR01.txt
# 03:20 running time

