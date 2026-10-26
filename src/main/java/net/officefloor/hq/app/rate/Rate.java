package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A sheet's rate for a grade: a percent with two decimals. */
public record Rate(String sheetId, String grade, BigDecimal rate) {
}
