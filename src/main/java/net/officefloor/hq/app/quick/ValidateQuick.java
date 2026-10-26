package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidateClaim;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateQuick {
    // The person form's rule for the name and the claim form's rules for the amount; the date is the
    // sheet's today and the note empty.
    public void validate(@RequestBody QuickRequest body, Out<QuickName> name, Out<ClaimInput> input)
            throws InvalidPersonException, InvalidClaimException {
        String typed = body == null || body.name() == null ? "" : body.name().trim();
        if (typed.isEmpty()) {
            throw new InvalidPersonException("Type a name.");
        }
        new ValidateClaim().validate(new ClaimRequest(body.amount(), "", "", ""), input);
        name.set(new QuickName(typed));
    }
}
