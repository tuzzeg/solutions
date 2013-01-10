package com.kaggle.acm.hackathon.word;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Responsibility of this class is to convert queries to the collection of words.
 *
 *  Rules for converting are the following:
 * <ol>
 * <li> The query is split to words based on {@link #SEPARATOR_PATTERN}</li>
 * <li> Each of the words in the query is then simplified: dots and trailing <i>'s</i> are removed from the words</li>
 * <li>
 * The numbers in format 20xx are changed to xx, so that 2012 is changed to 12. Idea behind is that for
 * year-versioned games like NHL or FIFA either way refers to the same game. Note that 2k12 is not changed to 12.
 * </li>
 * <li> If the word is believed to be year or version (see {@link #SEPARATOR_PATTERN}) than it is concatenated with previous</li>
 * <li> If the word concatenated with previous word appears to be a target spellchecker word, then the word is concatenated with previous</li>
 * </ol>

 */
public class Bagger {
    private static final String YEAR_OR_VERSION = "(2k)?\\d+";
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[: \\+-/\\(\\)]");
    private static final Splitter SPLITTER = Splitter.on(SEPARATOR_PATTERN).omitEmptyStrings().trimResults();

    private final SimpleSpellChecker checker;

    /**
     * Creates a new instance of bagger based on the spell checker passed.
     *
     * @param checker Spell checker to use. Can not be null.
     */
    public Bagger(SimpleSpellChecker checker) {
        this.checker = Preconditions.checkNotNull(checker);
    }

    /**
     * Converts the query passed to words.
     * Refer to class definition for the rules.
     *
     * @param query Query to process
     * @return Words extracted from query
     */
    public List<String> toBag(String query) {
        Preconditions.checkNotNull(query);

        List<String> ls = Lists.newArrayList();

        String prev = null;
        for (String term : SPLITTER.split(Dictionary.applyReplacements(query))) {
            String x = checker.correct(simplify(term));
            String u;
            if (prev != null && (x.matches(YEAR_OR_VERSION) || checker.isKnown(prev + x))) {
                u = prev + x;
            } else {
                u = x;
            }
            ls.addAll(Lists.newArrayList(checker.split(u)));
            prev = x;
        }
        return ls;
    }

    /**
     * Simplifies the word. Refer to class definition for simplification rules.
     *
     * @param word  Word to simplify
     * @return Simplified word.
     */
    private static String simplify(String word) {
        word = word.replaceAll("'s$", "").toLowerCase().replace(".", "");
        // fifa 2012 should match fifa 12
        if (word.matches("20\\d\\d")) {
            word = word.substring(2);
        }
        return word;
    }
}
