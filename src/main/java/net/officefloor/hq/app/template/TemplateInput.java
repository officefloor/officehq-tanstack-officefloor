package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A validated template form: the trimmed name, the amount (two decimals) and the trimmed note. */
public record TemplateInput(String name, BigDecimal amount, String note) {
}
