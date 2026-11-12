package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate change: the grade, its percent (two decimals), the date it applies from (null
 *  for from the beginning of time) and who sets it. */
public record RateInput(String grade, BigDecimal pct, LocalDate from, String user) {
}
