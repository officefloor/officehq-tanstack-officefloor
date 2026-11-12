package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template: its name (trimmed), the amount and the note a claim made from it starts with. */
public record Template(String name, BigDecimal amount, String note) {
}
