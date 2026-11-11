package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A person's plan on a sheet: an amount with two decimals, made a DRAFT claim on its start date, then
 * every {@code every} days, up to its end date. {@code generated} is how many claims it has made so far.
 */
public record Plan(Long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every,
        LocalDate end, int generated) {
}
