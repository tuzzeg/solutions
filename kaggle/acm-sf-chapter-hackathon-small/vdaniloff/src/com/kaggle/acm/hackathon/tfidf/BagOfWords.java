package com.kaggle.acm.hackathon.tfidf;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class BagOfWords {
    private final Map<Tag, Integer> words = Maps.newHashMap();

    public void count(Collection<Tag> words) {
        count(words, 1);
    }

    public void count(Collection<Tag> tags, int rank) {
        for (Tag tag : tags) {
            words.put(tag, getCount(tag) + rank);
        }
    }

    public Set<Tag> getWords() {
        return words.keySet();
    }

    public int getCount(Tag word) {
        return words.containsKey(word) ? words.get(word) : 0;
    }
}
