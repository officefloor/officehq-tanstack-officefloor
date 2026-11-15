package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for an amount the claim form would refuse, or a negative one (as for adding a plan). */
public class ValidatePlanAmount {
    public void validate(@RequestBody PlanAmountRequest request, Out<ValidPlanAmount> body) throws InvalidPlanException {
        if (request == null) throw new InvalidPlanException("An amount is required");
        ValidClaim amount;
        try {
            amount = ClaimForm.check(new ClaimRequest(request.amount(), "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        if (amount.isCorrection()) throw new InvalidPlanException("A plan's amount is not negative");
        body.set(new ValidPlanAmount(amount.amount()));
    }
}
