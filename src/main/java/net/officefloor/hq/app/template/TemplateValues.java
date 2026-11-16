package net.officefloor.hq.app.template;

/** A claim template to save once validated (see ValidateTemplate), and who saves it. */
public record TemplateValues(Template template, String by) {
}
