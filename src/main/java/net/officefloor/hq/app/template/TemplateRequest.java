package net.officefloor.hq.app.template;

/** Body of a template save: who is saving it, its name, the amount as typed and the note. */
public record TemplateRequest(String user, String name, String amount, String note) {
}
