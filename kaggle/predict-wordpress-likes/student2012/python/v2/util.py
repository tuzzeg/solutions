#topic_distribution_?: list of (topicid, topicvalue) tuples
def cosine_distance(topic_distribution_1, topic_distribution_2):
    if (len(topic_distribution_1) == 0) or (len(topic_distribution_2) == 0):
        return 0

    d1 = {}
    sum_of_squares_1 = 0
    for topic_id, topic_value in topic_distribution_1:
        d1[topic_id] = topic_value
        sum_of_squares_1 += topic_value ** 2

    numerator = 0
    sum_of_squares_2 = 0
    for topic_id, topic_value in topic_distribution_2:
        sum_of_squares_2 += topic_value ** 2
        if topic_id in d1:
            numerator += topic_value * d1[topic_id]

    return numerator / ((sum_of_squares_1 ** 0.5) * (sum_of_squares_2 ** 0.5))

def compute_similarity(topic_distribution_1, topic_distribution_2):
    return cosine_distance(topic_distribution_1, topic_distribution_2)

def get_pickle_file_suffix(populate_for_first_four_weeks):
    if populate_for_first_four_weeks:
        return '_wk_1_4'
    else:
        return '_wk_1_5'