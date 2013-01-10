package com.kaggle.acm.hackathon.word;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Map;

/**
 * This is a simple implementation of a spell checker.

 * The rules of spellchecking are the following:
 * <ol>
 *     <li>Words shorter than {@link #CORRECTION_TRESHOLD} are never corrected</li>
 *     <li>Words shorter than {@link #DOUBLE_CORRECTION_TRESHOLD} are only corrected if edit distance to correction is 0 or 1</li>
 *     <li>All other words are corrected if edit distance to correction is less or equal to 2</li>
 * </ol>
 *
 * We allow the following edits:
 * <ol>
 *     <li>Erasing a character (any character can be erased)</li>
 *     <li>Insertion of a letter or space</li>
 * </ol>
 *
 * This distance is different from Levenshtein distance.
 * E. g. the following changes are on the distance of two edits:
 * <ol>
 *     <li> Changing a character to allowed character</li>
 *     <li> Changing the order of two adjacent characters</li>
 * </ol>
 *
 * The words for correction are provided externally by using {@link #add(String)} or {@link #addCorrection(String, String)} methods.
 * Words or rules passed before will be returned as correction in case of edit distance tie.
 *
 * Internally spell checker is based on meet-in-the middle idea.
 */
public class SimpleSpellChecker {
    public static final String SPACE = " ";
    public static final Splitter SPLITTER = Splitter.on(SPACE);
    public static final int CORRECTION_TRESHOLD = 5;
    public static final int DOUBLE_CORRECTION_TRESHOLD = 6;

    Map<String, String> corrections = Maps.newHashMap();

    /**
     * Adds word to the correct words.
     * The corrections for words added before are not overwritten, so the order of passing words to the checker matters.
     *
     * @param word Possible correction word. Can not be null.
     */
    void add(String word) {
        Preconditions.checkNotNull(word);

        for (String misspelled : getMisspelled(word)) {
            if (!corrections.containsKey(misspelled))
                corrections.put(misspelled, word);
        }
        corrections.put(word, word);
    }

    /**
     * Adds a correction rule.
     * The difference with {@link #add(String)} is that only one correction is added
     * rather than adding all possible corrections to a word.
     *
     * @param word Misspelled word or phrase. Can not be null.
     * @param corr Correct word or phrase. Can not be null.
     */
    public void addCorrection(String word, String corr) {
        Preconditions.checkNotNull(word);
        Preconditions.checkNotNull(corr);

        corrections.put(word, corr);
    }

    /**
     * @param word Word to check. Can not be null.
     * @return Whether the word passed is known, i. e. is one of target words for correction
     */
    public boolean isKnown(String word) {
        Preconditions.checkNotNull(word);

        String s = corrections.get(word);
        return s != null && s.equals(word);
    }

    /**
     * Corrects the word based on the rules.
     * If the words has been corrected, then it is unpluralized.
     *
     * @param word Word to correct. Can not be null.
     * @return Corrected word. If correction is not known to the checker then the word itself is returned.
     */
    public String correct(String word) {
        Preconditions.checkNotNull(word);

        if (word.length() < CORRECTION_TRESHOLD || isKnown(word)) return word;

        String res = getCorrection(word);
        return res == null ? word : Dictionary.unpluralize(res);

    }

    /**
     * Returns a correction for the given word.
     * At first we check if there's a word on distance of one edit.
     * Then we check for words on distance on two edits, if allowed by rules.
     *
     * @param word Word to correct.
     * @return Corrected word. If no correction is found based on rules then null is returned.
     *
     * @see #DOUBLE_CORRECTION_TRESHOLD
     */
    private String getCorrection(String word) {
        String res = corrections.get(word);
        if (res != null) {
            return res;
        }

        for (String u : getMisspelled(word)) {
            String s = corrections.get(u);
            if (s != null && ((word.length() >= DOUBLE_CORRECTION_TRESHOLD) || isKnown(u))) {
                return s;
            }
        }
        return res;
    }

    /**
     * Generates possible misspells from the word.
     * Refer to edit rules in class definitions.
     *
     * @param word Word to generate misspells
     * @return Misspelled words
     */
    private Collection<String> getMisspelled(String word) {
        Collection<String> x = Sets.newHashSet();
        for (int i = 0; i < word.length(); i++) {
            String left = word.substring(0, i);
            String right = word.substring(i + 1);
            x.add(left + right);
            for (char c = 'a'; c <= 'z'; c++) {
                x.add(left + c + right);
                x.add(left + word.charAt(i) + c + right);
            }
            x.add(left + SPACE + right);
        }
        return x;
    }

    /**
     * Splits the word passed for the cases it was corrected to a phrase (words separated by a space).
     *
     * @param word Word or phrase to split
     * @return Splitted word
     */
    public Iterable<String> split(String word) {
        return SPLITTER.split(word);
    }
}
