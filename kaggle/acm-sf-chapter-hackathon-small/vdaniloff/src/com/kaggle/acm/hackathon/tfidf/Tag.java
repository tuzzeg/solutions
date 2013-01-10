package com.kaggle.acm.hackathon.tfidf;

import com.google.common.base.Preconditions;

import java.text.SimpleDateFormat;

public class Tag {
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private Type type;
    private String value;

    public enum Type {
        WORD,
        DATE,
        ALSO
    }

    public static Tag word(String value) {
        return new Tag(Type.WORD, value);
    }

    public static Tag date(long date) {
        return new Tag(Type.DATE, DATE_FORMAT.format(date));
    }

    public static Tag also(String value) {
        return new Tag(Type.ALSO, value);
    }

    private Tag(Type type, String value) {
        this.type = Preconditions.checkNotNull(type);
        this.value = Preconditions.checkNotNull(value);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;
        return type == tag.type && value.equals(tag.value);

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + value.hashCode();
        return result;
    }
}
