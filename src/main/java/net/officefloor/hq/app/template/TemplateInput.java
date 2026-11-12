package net.officefloor.hq.app.template;

/** A template to save, validated, and the user saving it. */
public record TemplateInput(Template template, String user) {
}
