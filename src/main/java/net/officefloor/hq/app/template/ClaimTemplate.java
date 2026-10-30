package net.officefloor.hq.app.template;

import java.math.BigDecimal;

/** A claim template: a claim made from it takes its amount and note as they are at that moment. */
public record ClaimTemplate(String name, BigDecimal amount, String note) {
}
