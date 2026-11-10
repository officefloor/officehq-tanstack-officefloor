package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template request, validated: its name trimmed, its amount and note as the claim form takes them. */
public record TemplateInput(String name, BigDecimal amount, String note) {
}
