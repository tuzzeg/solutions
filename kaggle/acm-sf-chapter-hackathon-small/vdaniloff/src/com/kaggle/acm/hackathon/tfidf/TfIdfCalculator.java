package com.kaggle.acm.hackathon.tfidf;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kaggle.acm.hackathon.data.Product;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TfIdfCalculator {
    final Map<Tag, Integer> docCount = Maps.newHashMap();
    final HashMap<Indexed<Product>, Integer> docs;

    class Comp implements Comparable<Comp> {
        double value;
        Product key;
        int count;

        Comp(double value, Indexed<Product> doc) {
            this.key = doc.getKey();
            this.value = value;
            this.count = docs.get(doc);
        }

        public int compareTo(Comp o) {
            if (value != o.value) {
                return value < o.value ? 1 : -1;
            }

            int res = Long.valueOf(count).compareTo((long) o.count);
            if (res == 0) {
                res = Integer.valueOf(key.getRank()).compareTo(o.key.getRank());
            }
            return res != 0 ? res : key.getSku().compareTo(o.key.getSku());
        }

    }

    public TfIdfCalculator(Map<Indexed<Product>, Integer> bags) {
        docs = Maps.newHashMap(bags);
        for (Indexed<Product> bag : docs.keySet()) {
            for (Tag word : bag.getWords()) {
                int count = !docCount.containsKey(word) ? 0 : docCount.get(word);
                docCount.put(word, count + 1);
            }
        }
    }

    public double getTfIdf(BagOfWords query, Indexed<Product> doc) {
        double res = 0;
        for (Tag word : query.getWords()) {
            double tfIdf = getTfIdf(word, doc);
            res += tfIdf;
        }
        return res;
    }

    private double getTfIdf(Tag word, Indexed<Product> doc) {
        if (!docCount.containsKey(word)) {
            return 0;
        }

        double tf = Math.log(1 + doc.getCount(word));
        double idf = Math.log(docs.size()) - Math.log(docCount.get(word));
        if (word.getType() == Tag.Type.ALSO) {
            tf *= 0.8;
        }
        return tf * idf;
    }

    public List<Product> getBest(BagOfWords query, long queryTime, int k) {
        TreeSet<Comp> set = Sets.newTreeSet();
        for (Indexed<Product> doc : docs.keySet()) {
            double tfIdf = getTfIdf(query, doc);

            if (query.getWords().contains(Tag.also(doc.getKey().getSku()))) {
                tfIdf *= 0;
            }

            long startDate = doc.getKey().getStartDate();
            if (startDate != Product.NO_DATE && !within(queryTime, startDate)) {
                tfIdf *= 0.0;
            }

            set.add(new Comp(tfIdf, doc));
            if (set.size() > k) {
                set.remove(set.last());
            }
        }

        List<Product> result = Lists.newArrayListWithCapacity(k);
        for (Comp min : set) {
            result.add(min.key);
        }

        return result;
    }

    private boolean within(long queryTime, long startDate) {
        return !(startDate > queryTime + TimeUnit.DAYS.toMillis(70) && startDate < queryTime - TimeUnit.DAYS.toMillis(365));
    }
}
