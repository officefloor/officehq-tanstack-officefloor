package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template of the sheet: its name, and the amount and note a claim made from it is given. */
public record ClaimTemplate(String sheetId, String name, BigDecimal amount, String note) {
}
