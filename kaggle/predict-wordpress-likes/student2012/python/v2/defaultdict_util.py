def increment_by_one(d, k1, k2):
    add_keys_if_missing(d, k1, k2)
    d[k1][k2] += 1

def get_value(d, k1, k2):
    if k1 not in d:
        return 0

    if k2 not in d[k1]:
        return 0

    return d[k1][k2]

def add_keys_if_missing(d, k1, k2):
    if k1 not in d:
        d[k1] = {}

    if k2 not in d[k1]:
        d[k1][k2] = 0
