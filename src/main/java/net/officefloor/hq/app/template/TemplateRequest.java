package net.officefloor.hq.app.template;

/** A claim template's fields as typed: name, amount and note, and the signed-in user (`by`) saving it. */
public record TemplateRequest(String name, String amount, String note, String by) {
}
