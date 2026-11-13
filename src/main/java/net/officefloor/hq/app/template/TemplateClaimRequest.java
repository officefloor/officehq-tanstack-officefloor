package net.officefloor.hq.app.template;

import jakarta.validation.constraints.Size;

/** A claim made from a template: the template's name. */
public record TemplateClaimRequest(@Size(max = 200) String template) {
}
