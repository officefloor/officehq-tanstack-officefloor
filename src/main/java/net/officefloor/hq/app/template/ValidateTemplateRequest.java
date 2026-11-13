package net.officefloor.hq.app.template;

import jakarta.validation.Valid;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a template with a blank name (once trimmed), or an amount the claim form would refuse for a claim
 * that corrects none. The note is kept as the claim form keeps it. */
public class ValidateTemplateRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid TemplateRequest request, Out<ClaimTemplate> template)
            throws InvalidTemplateException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("A template's name is required.");
        }
        ClaimInput input;
        try {
            input = ClaimForm.check(new ClaimRequest(request.amount(), "", request.note(), "", "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        template.set(new ClaimTemplate(sheet.id(), name, input.amount(), input.note()));
    }
}
