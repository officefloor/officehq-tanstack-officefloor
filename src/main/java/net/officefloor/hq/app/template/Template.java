package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template of the sheet: its name, and the amount and note a claim made from it takes. */
public record Template(String name, BigDecimal amount, String note) {
}
