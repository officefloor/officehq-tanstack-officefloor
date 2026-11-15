package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.claim.ValidateClaim;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Checks the name as the person form does and the amount as the claim form does (a blank date:
 *  the sheet's entry date when set, else its today; an empty note). */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<QuickAddName> name,
            Out<ValidClaim> claim) throws InvalidPersonException, InvalidClaimException {
        String n = request == null || request.name() == null ? "" : request.name().trim();
        if (n.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        new ValidateClaim().validate(new ClaimRequest(request.amount(), "", ""), claim);
        name.set(new QuickAddName(n));
    }
}
