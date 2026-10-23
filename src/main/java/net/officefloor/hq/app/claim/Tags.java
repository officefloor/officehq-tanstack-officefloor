package net.officefloor.hq.app.claim;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Tags as claims and people carry them. */
public final class Tags {
    private Tags() {
    }

    /** Tags as typed, separated by commas: each trimmed and lower-cased, blanks and repeats dropped, sorted and joined
     *  by commas ("" when none). */
    public static String normalise(String typed) {
        return Arrays.stream(typed.split(",")).map(t -> t.trim().toLowerCase(Locale.ROOT)).filter(t -> !t.isEmpty())
                .distinct().sorted().collect(Collectors.joining(","));
    }
}
