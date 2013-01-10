package com.kaggle.acm.hackathon;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.kaggle.acm.hackathon.data.Order;

import java.util.List;

public class OrderHistory {
    private HashMultimap<String, String> rp = HashMultimap.create();

    public OrderHistory(List<Order> orders) {
        for (Order o : orders) {
            rp.put(o.getUser(), o.getSku());
        }
    }

    public List<String> getOrdered(String user) {
        return Lists.newArrayList(rp.get(user));
    }

}
