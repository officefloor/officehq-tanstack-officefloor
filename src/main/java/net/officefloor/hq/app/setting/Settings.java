package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/**
 * A sheet's settings: its clerk limit, the highest due a clerk may settle (null: no limit), its
 * floor, the least a claim's live due may be, and its terms, the days after a claim's date that it
 * is due by, and its late percent, what an overdue claim's due gains per full 30 days overdue.
 */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, BigDecimal latePct) {
    /** A sheet's floor until it is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** A sheet's terms until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** A sheet's late percent until it is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** Whether a due (null: n/a) is within the clerk limit. A due of n/a is within any limit. */
    public boolean withinClerkLimit(BigDecimal due) {
        return clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
