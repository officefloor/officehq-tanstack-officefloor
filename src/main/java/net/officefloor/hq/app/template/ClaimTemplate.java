package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template on the sheet: its name, the amount (two decimals) and the note a claim made from it gets. */
public record ClaimTemplate(String sheetId, String name, BigDecimal amount, String note) {
}
