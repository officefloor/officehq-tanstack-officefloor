package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template on a sheet: its name (trimmed), amount (two decimals) and note, and who saved it last. */
public record ClaimTemplate(String sheetId, String name, BigDecimal amount, String note, String by) {
}
