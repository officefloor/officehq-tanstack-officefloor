package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A sheet's settings: its clerk limit, the highest due a clerk may settle (null: no limit). */
public record Settings(String sheetId, BigDecimal clerkLimit) {
    /** Whether a due (null: n/a) is within the clerk limit. A due of n/a is within any limit. */
    public boolean withinClerkLimit(BigDecimal due) {
        return clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
