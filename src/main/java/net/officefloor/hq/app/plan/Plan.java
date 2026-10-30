package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: the amount, from the start every N days up to the end; generated is how many claims it has made. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end, int generated) {
}
