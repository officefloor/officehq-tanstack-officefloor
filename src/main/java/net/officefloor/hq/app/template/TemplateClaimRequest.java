package net.officefloor.hq.app.template;

/** Body of POST /api/sheets/{sheet}/people/{person}/claims/template: the name of the template the claim is made from. */
public record TemplateClaimRequest(String template) {
}
