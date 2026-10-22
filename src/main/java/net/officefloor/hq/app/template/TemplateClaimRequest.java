package net.officefloor.hq.app.template;

/** Body of a claim made from a template: who is making it and the template's name. */
public record TemplateClaimRequest(String user, String template) {
}
