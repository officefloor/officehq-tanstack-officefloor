package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/**
 * A sheet's settings: its clerk limit, the highest due a clerk may settle (null: no limit), and its
 * floor, the least a claim's live due may be.
 */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor) {
    /** A sheet's floor until it is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** Whether a due (null: n/a) is within the clerk limit. A due of n/a is within any limit. */
    public boolean withinClerkLimit(BigDecimal due) {
        return clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
