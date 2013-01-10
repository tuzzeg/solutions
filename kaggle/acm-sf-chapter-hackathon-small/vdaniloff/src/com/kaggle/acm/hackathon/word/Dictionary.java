package com.kaggle.acm.hackathon.word;

import java.util.Arrays;
import java.util.List;

/**
 * This class has hardcoded words for corrections, replacements and unpluralization.
 * The words and correction rules where selected manually.
 */
public class Dictionary {
    private static final List<String> words = Arrays.asList(
            "asylum",
            "arkham",
            "bioshock",
            "gears",
            "warfare",
            "warfare3",
            "brotherhood",
            "redemption",
            "creed",
            "company",
            "cursed",
            "marines",
            "kinect",
            "skylanders",
            "black ops",
            "guitar",
            "guitar hero",
            "legendary",
            "spider",
            "addition",
            "insect",
            "island",
            "skyrim",
            "forza4",
            "tenkaichi",
            "pirates",
            "assassins",
            "assassin",
            "monopoly",
            "rocksmith",
            "fifa",
            "battle",
            "field",
            "battlefield",
            "battlefield3",
            "batman",
            "borderlands",
            "borderlands2",
            "bulletstorm",
            "midnight club",
            "katmai",
            "rayman",
            "assault",
            "scrolls",
            "personal",
            "collectors",
            "collector",
            "editor",
            "warrior",
            "edition",
            "central",
            "advanced",
            "transformers",
            "wipeout",
            "deadrising",
            "deadrising2",
            "motionsports",
            "motorsport",
            "motorsports",
            "motorsports4",
            "motorsport4",
            "wolfenstein",
            "turtle beaches",
            "ultimate",
            "unleashed",
            "forza",
            "gunstringer",
            "destiny",
            "warriors",
            "hardened",
            "infinite",
            "modern warfare",
            "modern",
            "revelations",
            "revelation",
            "evolved",
            "revelation",
            "limited",
            "star wars",
            "cabelas",
            "calibur",
            "tekken",
            "warhammer",
            "hitman",
            "jurassic",
            "madden",
            "just dance",
            "collection",
            "legendary",
            "crysis",
            "dragonball",
            "dragonball z",
            "midnight",
            "minecraft",
            "oblivion",
            "rocksmith",
            "homefront",
            "forza motor",
            "forza",
            "noire",
            "soldier",
            "xbox 360",
            "trainer",
            "spiderman",
            "sniper",
            "raider",
            "saboteur",
            "katamari",
            "anniversary",
            "steering",
            "zumba",
            "resident",
            "champion",
            "san francisco",
            "honor",
            "kinetic",
            "wheel",
            "effect",
            "cybertron",
            "dynasty",
            "allstars",
            "yoostar",
            "francisco",
            "shift",
            "shaddai",
            "splatterhouse",
            "deus",
            "xmen"
    );

    public static final int PLURAL_TRESHOLD = 8;

    /**
     * Creates a simple spell checker based on hardcoded dictionary words and correction rules.
     *
     * @return Spell checker.
     */
    public static SimpleSpellChecker createChecker() {
        SimpleSpellChecker checker = new SimpleSpellChecker();
        for (String s : words) {
            checker.add(s);
        }

        checker.addCorrection("harden", "hardened");
        checker.addCorrection("froza", "forza");
        checker.addCorrection("intent", "intent");
        checker.addCorrection("editon", "edition");
        checker.addCorrection("infinity", "infinity");
        checker.addCorrection("connection", "connection");
        checker.addCorrection("riders", "riders");
        checker.addCorrection("reader", "reader");
        checker.addCorrection("island", "island");
        checker.addCorrection("40000", "40k");
        checker.addCorrection("years", "years");
        checker.addCorrection("gear", "gear");
        checker.addCorrection("hearts", "hearts");
        checker.addCorrection("dragonballz", "dragonball z");
        checker.addCorrection("dues", "deus");
        checker.addCorrection("control", "control");
        checker.addCorrection("string", "string");
        checker.addCorrection("compact", "compact");
        checker.addCorrection("command", "command");
        checker.addCorrection("plates", "plates");
        checker.addCorrection("froze", "forza");

        return checker;
    }

    static String applyReplacements(String query) {
        query = query.replace("dead rising", "deadrising");
        return query;
    }

    /**
     * If the passed word is recognized as plural then the word is converted to single form.
     * The logic behind the pluralization is that all words longer than {@link #PLURAL_TRESHOLD} ending with 's' are
     * treated as plural and then converted to single form. However, there are hardcoded exceptions to the rules.
     * All other words are returned as is.
     *
     * @param word
     * @return Word converted to the single form, or passed word itself in case it is not recognized as plural.
     */
    static String unpluralize(String word) {
        if (word.startsWith("border") || word.startsWith("motion")) return word;
        return word.length() > PLURAL_TRESHOLD && !word.contains(" ") && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
    }
}
