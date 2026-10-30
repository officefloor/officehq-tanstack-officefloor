package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template that passed validation: a trimmed, non-blank name and an amount the claim form takes. */
public record ValidTemplate(String name, BigDecimal amount, String note, String user) {
}
