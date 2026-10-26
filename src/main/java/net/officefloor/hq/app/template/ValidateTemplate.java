package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimAmount;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** A claim template's fields: a name (trimmed, required), an amount by the claim form's rule and a note (trimmed). */
public class ValidateTemplate {

    public void validate(@Val SheetResponse sheet, @RequestBody TemplateRequest request, Out<Template> template,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Enter a template name");
        }
        if (name.length() > 200) {
            throw new BadRequestException("A template name is at most 200 characters");
        }
        BigDecimal amount = ClaimAmount.signed(request == null ? null : request.amount());
        if (note.length() > 1000) {
            throw new BadRequestException("A note is at most 1000 characters");
        }
        template.set(new Template(sheet.id(), name, amount, note));
        changedBy.set(by);
    }
}
