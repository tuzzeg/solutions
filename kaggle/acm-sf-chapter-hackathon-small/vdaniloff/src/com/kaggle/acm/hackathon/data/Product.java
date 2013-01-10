package com.kaggle.acm.hackathon.data;

/**
 * POJO class describing BestBuy product.
 */
public class Product {
    public static final long NO_DATE = 0L;
    public static final int NO_RANK = 100000;

    private final String name;
    private final String sku;
    private final long startDate;
    private final int rank;

    /**
     * Creates a product with given parameters.
     *
     * @param name Product name (short name in XML)
     * @param sku Product SKU
     * @param startDate Minimum of product sell start or release date. If not provided then it is {@link #NO_DATE}.
     * @param rank Selling rank (mid-term selling rank from XML)
     */
    public Product(String name, String sku, long startDate, int rank) {
        this.name = name;
        this.sku = sku;
        this.startDate = startDate;
        this.rank = rank;
    }

    /**
     * @return Product name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Product start date, or {@link #NO_DATE} if it is not provided.
     */
    public long getStartDate() {
        return startDate;
    }

    /**
     * @return Product SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * @return Best selling rank
     */
    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        if (name != null ? !name.equals(product.name) : product.name != null) return false;
        if (sku != null ? !sku.equals(product.sku) : product.sku != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        return result;
    }
}
