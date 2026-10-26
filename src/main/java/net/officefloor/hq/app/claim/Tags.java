package net.officefloor.hq.app.claim;

/** Tags as typed (comma separated), as a claim or a person carries them. */
public final class Tags {

    private Tags() {
    }

    /** The tags as typed, normalised: trimmed, lower-cased, without repeats, sorted and joined by commas; null when
     *  there are none. */
    public static String normalise(String typed) {
        if (typed == null) {
            return null;
        }
        java.util.SortedSet<String> tags = new java.util.TreeSet<>();
        for (String tag : typed.split(",")) {
            String t = tag.trim().toLowerCase(java.util.Locale.ROOT);
            if (!t.isEmpty()) {
                tags.add(t);
            }
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }
}
