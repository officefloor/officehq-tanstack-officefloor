package net.officefloor.hq.app.template;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The template a claim is made from: its name (trimmed, required). */
public class ValidateTemplateClaim {
    public void validate(@RequestBody TemplateClaimRequest request, Out<TemplateName> template,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = request == null || request.template() == null ? "" : request.template().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Choose a template");
        }
        template.set(new TemplateName(name));
        changedBy.set(by);
    }
}
