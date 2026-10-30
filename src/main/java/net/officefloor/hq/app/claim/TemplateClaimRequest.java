package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/template-claims: the template to add a claim from. */
public record TemplateClaimRequest(String template, String user) {
}
