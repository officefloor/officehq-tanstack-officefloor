package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A grade's rate on a sheet: a percent with two decimals. */
public record Rate(String sheetId, String grade, BigDecimal pct) {
}
