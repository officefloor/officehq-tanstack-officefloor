package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** The rate a sheet has for a grade: a percent with two decimals. */
public record Rate(String sheetId, String grade, BigDecimal rate) {
}
