package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: its amount (two decimals), start, every N days and end, who added it, and how many
 *  claims it has made so far. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every,
        LocalDate end, String by, int generated) {
}
