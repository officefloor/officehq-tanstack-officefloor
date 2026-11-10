package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Checks the name by the person form's rules and the amount by the claim form's: dated today, no note. */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<QuickAdd> quickOut, Out<ClaimInput> claimOut)
            throws InvalidPersonException, InvalidClaimException {
        String name = PersonRequest.checkName(request == null ? null : request.name());
        ClaimInput claim = ClaimInput.from(new ClaimRequest(request.amount(), "", "", ""));
        quickOut.set(new QuickAdd(name));
        claimOut.set(claim);
    }
}
