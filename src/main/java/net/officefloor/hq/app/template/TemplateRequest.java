package net.officefloor.hq.app.template;

import jakarta.validation.constraints.Size;

/** A claim template as the form submits it: its name, amount and note. */
public record TemplateRequest(@Size(max = 200) String name, @Size(max = 30) String amount, @Size(max = 1000) String note) {
}
