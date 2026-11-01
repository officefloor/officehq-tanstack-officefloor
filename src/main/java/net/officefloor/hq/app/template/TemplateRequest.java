package net.officefloor.hq.app.template;

/** Body of POST /api/sheets/{sheet}/templates: the template saved (a name saved again replaces it). */
public record TemplateRequest(String name, String amount, String note) {
}
