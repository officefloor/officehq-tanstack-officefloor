package net.officefloor.hq.app.template;

import java.util.List;

/** The claim templates on a sheet, by name. */
public record TemplatesResponse(List<TemplateResponse> templates) {
}
