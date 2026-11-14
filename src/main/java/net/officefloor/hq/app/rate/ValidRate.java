package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A rate request that passed validation: a known grade, a percent scaled to two decimals and its
 *  from-date (null for none). */
public record ValidRate(String grade, BigDecimal pct, LocalDate from) {
}
