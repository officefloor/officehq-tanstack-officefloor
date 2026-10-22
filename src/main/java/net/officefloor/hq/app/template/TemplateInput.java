package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A validated template save: who is saving it, its name and note, trimmed, and the amount the claim form accepts. */
public record TemplateInput(String user, String name, BigDecimal amount, String note) {
}
