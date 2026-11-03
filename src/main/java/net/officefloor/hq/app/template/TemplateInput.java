package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A validated template form: the name (trimmed), the amount (two decimals) and the note (trimmed). */
public record TemplateInput(String name, BigDecimal amount, String note) {
}
