from collections import defaultdict
import random
from v1.config_and_pickle import unpickle
from v2 import defaultdict_util
from v2.util import compute_similarity, get_pickle_file_suffix

class train_feature_util:
    def __init__(self, populate_for_first_four_weeks):
        self.blog_post_count_map         = unpickle('blog_post_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.blog_author__post_count_map = unpickle('blog_author__post_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.user_likes_map              = unpickle('user_likes_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.user_liked_blogs_map        = unpickle('user_liked_blogs_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.blog_liked_users_map        = unpickle('blog_liked_users_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.tag_blog_count_map          = unpickle('tag_blog_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.category_blog_count_map     = unpickle('category_blog_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.tag_user_count_map          = unpickle('tag_user_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
        self.category_user_count_map     = unpickle('category_user_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))

        self.blog_topic_distribution_map = unpickle('blog_topic_distribution_map')

    def find_topic_distribution(self, blog, post_id):
        return self.blog_topic_distribution_map['' + blog + '_' + post_id]

    #returns *Set* of blogs liked by specified user
    def liked_blogs(self, uid):
        return self.user_liked_blogs_map[uid]

    def like_count(self, uid):
        return len(self.user_likes_map[uid])

    def cosine_similarity(self, uid, max_post_to_compare, topic_distribution):
        liked_blog_post_tuples = self.user_likes_map[uid]

        liked_blog_post_tuples__sample = liked_blog_post_tuples
        if len(liked_blog_post_tuples__sample) > max_post_to_compare:
            random.shuffle(liked_blog_post_tuples)
            liked_blog_post_tuples__sample = liked_blog_post_tuples[0:max_post_to_compare]

        max_cosine_similarity = -1
        avg_cosine_similarity = 0
        for blog, post_id, author in liked_blog_post_tuples__sample:
            similarity = compute_similarity(self.find_topic_distribution(blog, post_id), topic_distribution)
            avg_cosine_similarity += similarity
            if similarity > max_cosine_similarity:
                max_cosine_similarity = similarity

        avg_cosine_similarity /= len(liked_blog_post_tuples__sample)

        return (max_cosine_similarity, avg_cosine_similarity)

    #returs *Set* of users
    def users_who_have_liked_this_blog_before(self, blog):
        return self.blog_liked_users_map[blog]

    #returns a tuple of (max, average) of tag_like_fractions for the specified tags
    def get_tag_like_fractions(self, uid, tags):
        if (len(tags) == 0):
            return [-1, -1]

        tag_like_fractions = [self.get_tag_like_fraction(uid, tag) for tag in tags]
        return [max(tag_like_fractions), sum(tag_like_fractions)/len(tag_like_fractions)]

    def get_tag_like_fraction(self, uid, tag):
        #how many posts has user liked for this tag
        numerator = defaultdict_util.get_value(self.tag_user_count_map, tag, uid)
        
        #how many total posts were made with this tag in all the blogs read by user
        denominator = 0
        for blog in self.user_liked_blogs_map[uid]:
            #denominator += self.tag_blog_count_map[tag][blog]
            denominator += defaultdict_util.get_value(self.tag_blog_count_map, tag, blog)

#        if denominator == 0:
#            return 0

        denominator += 1

        return 1.0 * numerator / denominator

    #returns a tuple of (max, average) of category_like_fractions for the specified categories
    def get_category_like_fractions(self, uid, categories):
        if (len(categories) == 0):
            return [-1, -1]

        category_like_fractions = [self.get_category_like_fraction(uid, category) for category in categories]
        return [max(category_like_fractions), sum(category_like_fractions)/len(category_like_fractions)]

    def get_category_like_fraction(self, uid, category):
        #how many posts has user liked for this category
        numerator = defaultdict_util.get_value(self.category_user_count_map, category, uid)

        #how many total posts were made with this category in all the blogs read by user
        denominator = 0
        for blog in self.user_liked_blogs_map[uid]:
            denominator += defaultdict_util.get_value(self.category_blog_count_map, category, blog)

#        if denominator == 0:
#            return 0

        denominator += 1

        return 1.0 * numerator / denominator

    def get_blog_like_fraction(self, uid, blog):
        return self.get_blog_like_fraction_map(uid)[blog]
    
    def get_blog_like_fraction_map(self, uid):
        blog_liked_post_count_map = defaultdict(int)
        for blog, post_id, author in self.user_likes_map[uid]:
            blog_liked_post_count_map[blog] += 1

        blog_like_fraction_map = {}
        for blog, liked_post_count in blog_liked_post_count_map.iteritems():
            blog_like_fraction_map[blog] = 1.0 * liked_post_count / self.blog_post_count_map[blog]

        return blog_like_fraction_map

    def get_blog_author_like_fraction(self, uid, blog, author):
        return self.get_blog_author_like_fraction_map(uid)[blog + '_' + author]

    def get_blog_author_like_fraction_map(self, uid):
        #key   = blog + '_' + author
        #value = liked_post_count
        blog_author_liked_post_count_map = defaultdict(int)
        for blog, post_id, author in self.user_likes_map[uid]:
            blog_author_liked_post_count_map[blog + '_' + author] += 1

        #key   = blog + '_' + author
        #value = like_fraction
        blog_author_like_fraction_map = defaultdict(int)
        for blog_author, liked_post_count in blog_author_liked_post_count_map.iteritems():
            blog_author_like_fraction_map[blog_author] = 1.0 * liked_post_count / self.blog_author__post_count_map[blog_author]

        return blog_author_like_fraction_map