package com.kaggle.acm.hackathon.data;

public class Request {
    private final long entered;
    private final String query;

    public Request(String query, long entered) {
        this.query = query;
        this.entered = entered;
    }

    public long getEntered() {
        return entered;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (entered != request.entered) return false;
        if (query != null ? !query.equals(request.query) : request.query != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (entered ^ (entered >>> 32));
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Request{" +
                "entered=" + entered +
                ", query='" + query + '\'' +
                '}';
    }
}
