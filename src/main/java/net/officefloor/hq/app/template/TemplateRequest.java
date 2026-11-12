package net.officefloor.hq.app.template;

/** Body of POST /api/sheets/{sheetId}/templates: the name, amount and note as typed, and the user who saves it. */
public record TemplateRequest(String name, String amount, String note, String user) {
}
