package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: a DRAFT claim of the amount on every plan date, the start then every {@code every}
 *  days, up to the end; {@code generated} how many claims it has made so far. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every,
        LocalDate end, int generated) {
}
