package net.officefloor.hq.app.template;

/** The template form as submitted: its name, amount and note. */
public record TemplateRequest(String name, String amount, String note) {
}
