package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template of a sheet: its name (trimmed), an amount the claim form accepts and a note (may be empty). */
public record Template(String sheetId, String name, BigDecimal amount, String note) {
}
