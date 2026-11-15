package net.officefloor.hq.app.template;

/** Body of saving a claim template: its name, amount and note. */
public record TemplateRequest(String name, String amount, String note) {
}
