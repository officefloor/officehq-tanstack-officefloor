package net.officefloor.hq.app.template;

/** The body of saving a claim template, as typed: its name, amount and note. */
public record TemplateRequest(String name, String amount, String note) {
}
