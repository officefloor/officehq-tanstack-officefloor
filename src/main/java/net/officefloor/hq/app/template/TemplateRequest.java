package net.officefloor.hq.app.template;

/** Body of POST /api/sheets/{sheetId}/templates: the template to save (a name saved again is replaced). */
public record TemplateRequest(String name, String amount, String note, String user) {
}
