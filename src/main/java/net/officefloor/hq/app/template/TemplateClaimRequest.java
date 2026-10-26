package net.officefloor.hq.app.template;

/** The name of the template a claim is made from, and the signed-in user (`by`) making it. */
public record TemplateClaimRequest(String template, String by) {
}
