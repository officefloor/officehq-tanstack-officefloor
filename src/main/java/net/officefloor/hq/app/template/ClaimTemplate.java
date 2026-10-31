package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template on a sheet: its name, amount and note, saved by `by`. */
public record ClaimTemplate(String sheetId, String name, BigDecimal amount, String note, String by) {
}
