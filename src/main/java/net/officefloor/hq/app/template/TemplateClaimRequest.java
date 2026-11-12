package net.officefloor.hq.app.template;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/template-claims: the template's name and the user
 *  who adds the claim. */
public record TemplateClaimRequest(String template, String user) {
}
