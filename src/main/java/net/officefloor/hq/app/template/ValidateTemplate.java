package net.officefloor.hq.app.template;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the template form: a name is required (trimmed); the amount is one the claim form would take with
 *  nothing to correct (0.00 or more, up to two decimals); the note may be empty. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateInput> template) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        template.set(new TemplateInput(name, ClaimInput.amount(amount), note));
    }
}
