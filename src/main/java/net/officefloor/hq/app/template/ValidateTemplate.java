package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the name, trimmed, is not blank; the amount is one the claim form takes for a claim that corrects
 *  nothing (a number 0.00 or more with up to two decimals, see ClaimValues); the note is trimmed as the form trims it. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateValues> values) throws InvalidTemplateException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("A template's name must not be blank");
        }
        ClaimValues claim;
        try {
            claim = ClaimValues.from(new ClaimRequest(request.amount(), null, request.note(), null, request.by(), null, null));
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        if (claim.amount().signum() < 0) {
            throw new InvalidTemplateException("A template's amount must be 0.00 or more");
        }
        values.set(new TemplateValues(new Template(name, claim.amount(), claim.note()), claim.by()));
    }
}
