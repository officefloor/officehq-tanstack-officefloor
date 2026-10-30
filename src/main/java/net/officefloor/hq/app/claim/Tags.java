package net.officefloor.hq.app.claim;

/** Tags as claims and people carry them. */
public final class Tags {

    private Tags() {
    }

    /** Tags from a form's comma separated text: each trimmed and lower-cased, blanks and repeats
     *  dropped, sorted and joined by commas; null when there are none. */
    public static String normalise(String raw) {
        if (raw == null) return null;
        java.util.TreeSet<String> tags = new java.util.TreeSet<>();
        for (String tag : raw.split(",")) {
            String t = tag.trim().toLowerCase(java.util.Locale.ROOT);
            if (!t.isEmpty()) tags.add(t);
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }
}
