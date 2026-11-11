package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A plan's new amount is one the claim form would take. */
public class ValidatePlanAmount {
    public void validate(@RequestBody PlanAmountRequest request, Out<PlanAmount> body) throws InvalidPlanException {
        if (request == null) throw new InvalidPlanException("A plan's new amount is required");
        try {
            body.set(new PlanAmount(ClaimInput.from(new ClaimRequest(request.amount(), "", "", "", "", "")).amount()));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
    }
}
