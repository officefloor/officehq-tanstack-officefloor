package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Applies the person form's and the claim form's rules to the quick-add bar: the claim it adds is
 *  dated the sheet's today with an empty note. */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<QuickAdd> quick, Out<ValidClaim> claim)
            throws InvalidPersonException, InvalidClaimException {
        String name = request == null ? null : request.name();
        if (name == null || name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        claim.set(ValidateClaim.of(new ClaimRequest(request.amount(), null, "")));
        // Stored without surrounding spaces, as the person form does.
        quick.set(new QuickAdd(name.strip()));
    }
}
