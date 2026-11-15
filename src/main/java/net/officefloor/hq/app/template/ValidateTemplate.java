package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a blank name, and for an amount (and note) the claim form would refuse. Publishes the
 *  template's name (trimmed) with its amount and note as the claim form keeps them. */
public class ValidateTemplate {
    public void validate(@RequestBody TemplateRequest request, Out<TemplateRequest> body)
            throws InvalidClaimException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidClaimException("A template needs a name");
        }
        ValidClaim checked = ClaimForm.check(new ClaimRequest(request.amount(), "", request.note()));
        body.set(new TemplateRequest(name, checked.amount().toPlainString(), checked.note()));
    }
}
