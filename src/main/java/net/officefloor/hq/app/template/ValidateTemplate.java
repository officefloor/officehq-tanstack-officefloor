package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A template needs a name (kept trimmed), and an amount and note the claim form would take for an undated claim. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateInput> body)
            throws InvalidTemplateException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new InvalidTemplateException("A template name is required");
        if (name.length() > 200) throw new InvalidTemplateException("A template name is at most 200 characters");
        ClaimInput claim;
        try {
            claim = ClaimInput.from(new ClaimRequest(request.amount(), "", request.note(), "", "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        body.set(new TemplateInput(name, claim.amount(), claim.note()));
    }
}
