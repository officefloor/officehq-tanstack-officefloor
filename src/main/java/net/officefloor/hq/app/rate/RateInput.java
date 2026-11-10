package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate request: its grade, its percent scaled to two decimals, its from-date (or null) and its to-date (or null). */
public record RateInput(String grade, BigDecimal pct, LocalDate from, LocalDate to) {
}
