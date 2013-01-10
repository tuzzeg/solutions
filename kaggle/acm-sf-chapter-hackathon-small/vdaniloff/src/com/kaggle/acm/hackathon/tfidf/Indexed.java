package com.kaggle.acm.hackathon.tfidf;

import java.util.Collection;
import java.util.Set;

public class Indexed<T> {
    private final BagOfWords impl;
    private final T key;

    public Indexed(T key) {
        this.key = key;
        this.impl = new BagOfWords();
    }

    public void count(Collection<Tag> words, int rank) {
        impl.count(words, rank);
    }

    public void count(Collection<Tag> words) {
        impl.count(words);
    }

    public Set<Tag> getWords() {
        return impl.getWords();
    }

    public T getKey() {
        return key;
    }

    public int getCount(Tag word) {
        return impl.getCount(word);
    }
}
