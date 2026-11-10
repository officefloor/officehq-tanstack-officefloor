package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate request: its grade, its percent scaled to two decimals, and its from-date (or null). */
public record RateInput(String grade, BigDecimal pct, LocalDate from) {
}
