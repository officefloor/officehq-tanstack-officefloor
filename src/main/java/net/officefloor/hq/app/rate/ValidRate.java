package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A rate that passed validation: the grade and its percent at two decimals. */
public record ValidRate(String grade, BigDecimal rate, String user) {
}
