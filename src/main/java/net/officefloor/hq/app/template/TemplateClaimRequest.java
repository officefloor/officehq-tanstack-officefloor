package net.officefloor.hq.app.template;

/** Body of adding a claim from a template: the template's name, and who adds the claim. */
public record TemplateClaimRequest(String template, String by) {
}
