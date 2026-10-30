package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A rate that passed validation: the grade, its percent at two decimals and the date it applies
 *  from (null for from the beginning of time) and through (null for no end). */
public record ValidRate(String grade, BigDecimal rate, LocalDate from, LocalDate to, String user) {
}
