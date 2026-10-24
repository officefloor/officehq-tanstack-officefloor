package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the template form from the body; refuses a blank name, and an amount (with its note) the claim form would
 *  refuse. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateInput> body) throws InvalidTemplateException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("A template needs a name.");
        }
        ClaimInput claim;
        try {
            claim = ClaimRules.check(request.amount(), null, request.note(), null, null);
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        body.set(new TemplateInput(name, claim.amount(), claim.note()));
    }
}
