package net.officefloor.hq.app.claim;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** A claim's tags as stored: trimmed, lower-cased, without repeats, sorted and comma separated; null for none. */
public final class Tags {

    private Tags() {
    }

    /** The tags of a comma separated text (null: blank), as stored; null when it names none. */
    public static String normalise(String field) {
        if (field == null) {
            return null;
        }
        TreeSet<String> tags = new TreeSet<>();
        for (String t : field.split(",")) {
            String tag = t.trim().toLowerCase(Locale.ROOT);
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    /** The stored tags (null: none) as a sorted list. */
    public static List<String> list(String stored) {
        return stored == null || stored.isEmpty() ? List.of() : Arrays.asList(stored.split(","));
    }
}
