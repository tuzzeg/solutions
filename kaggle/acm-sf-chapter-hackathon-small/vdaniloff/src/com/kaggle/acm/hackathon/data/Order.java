package com.kaggle.acm.hackathon.data;

/**
 * This POJO class describes the order that user made.
 */
public class Order {
    private final Request request;
    private final String sku;
    private final String user;

    /**
     * Creates an order for the user.
     *
     * @param query Entered query
     * @param sku Sku ordered
     * @param time Time of request
     * @param user User id
     */
    public Order(String query, String sku, long time, String user) {
        this.request = new Request(query, time);
        this.sku = sku;
        this.user = user;
    }

    @Override
    public String toString() {
        return "Order{" +
                "request='" + request + '\'' +
                ", sku='" + sku + '\'' +
                '}';
    }

    /**
     * @return Sku of this order
     */
    public String getSku() {
        return sku;
    }

    /**
     * @return Id of user, that made the order
     */
    public String getUser() {
        return user;
    }

    /**
     * @return Search request user made
     */
    public Request getRequest() {
        return request;
    }
}
