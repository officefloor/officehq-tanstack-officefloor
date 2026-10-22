package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a name that is not blank (at
 *  most 200 characters), and an amount and note the claim form would accept for a claim with no date. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateInput> body) throws InvalidTemplateException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidTemplateException("Sign in before saving a template.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("Give the template a name.");
        }
        if (name.length() > 200) {
            throw new InvalidTemplateException("A template name is at most 200 characters.");
        }
        ClaimInput claim;
        try {
            claim = ClaimFormRules.check(new ClaimRequest(request.user(), request.amount(), "", request.note(), "", "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        body.set(new TemplateInput(claim.user(), name, claim.amount(), claim.note()));
    }
}
