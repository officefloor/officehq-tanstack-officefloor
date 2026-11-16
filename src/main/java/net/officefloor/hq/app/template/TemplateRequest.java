package net.officefloor.hq.app.template;

/** Body of saving a claim template: its name, amount and note, and who saves it. */
public record TemplateRequest(String name, String amount, String note, String by) {
}
