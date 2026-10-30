package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The person form's and the claim form's rules, applied to the quick-add bar. */
public class ValidateQuickClaim {
    public void validate(@RequestBody QuickClaimRequest request, Out<ValidQuickClaim> body)
            throws InvalidPersonException, InvalidClaimException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        body.set(new ValidQuickClaim(name, ValidateClaim.amount(request.amount()), request.user()));
    }
}
