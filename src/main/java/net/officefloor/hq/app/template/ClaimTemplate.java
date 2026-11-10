package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template on a sheet: its name (trimmed), an amount with two decimals and a note. */
public record ClaimTemplate(String sheetId, String name, BigDecimal amount, String note) {
}
