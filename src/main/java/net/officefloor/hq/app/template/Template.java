package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template on a sheet: its name, the amount and note a claim made from it is given, and who last saved it. */
public record Template(String sheetId, String name, BigDecimal amount, String note, String by) {
}
