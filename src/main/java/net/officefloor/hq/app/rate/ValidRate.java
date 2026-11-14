package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A rate request that passed validation: a known grade, a percent scaled to two decimals and its
 *  from-date and to-date (null for none; the to-date is not before the from-date). */
public record ValidRate(String grade, BigDecimal pct, LocalDate from, LocalDate to) {
}
