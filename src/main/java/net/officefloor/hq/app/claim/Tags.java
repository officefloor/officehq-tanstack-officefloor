package net.officefloor.hq.app.claim;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** A claim's tags: typed comma separated, each trimmed and lower-cased, blanks and repeats dropped; stored
 *  sorted and comma separated (no spaces), null when there are none. */
public final class Tags {
    private Tags() {
    }

    /** The typed tags as stored; null when none were given. */
    public static String normalise(String typed) {
        if (typed == null) {
            return null;
        }
        TreeSet<String> tags = new TreeSet<>();
        for (String tag : typed.split(",")) {
            String t = tag.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                tags.add(t);
            }
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    /** The stored tags, sorted; empty when there are none. */
    public static List<String> list(String stored) {
        return stored == null || stored.isEmpty() ? List.of() : Arrays.asList(stored.split(","));
    }
}
