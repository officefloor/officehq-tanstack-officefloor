package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate set: who is making it, the grade, the percent with two decimals, and the from-date
 *  (null when none was given). */
public record RateInput(String user, String grade, BigDecimal pct, LocalDate from) {
}
