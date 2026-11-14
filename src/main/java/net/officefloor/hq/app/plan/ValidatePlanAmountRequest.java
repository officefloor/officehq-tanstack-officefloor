package net.officefloor.hq.app.plan;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the new amount the plan's row submits; a body with none is refused (400). */
public class ValidatePlanAmountRequest {
    public void validate(@RequestBody @Valid PlanAmountRequest request, Out<PlanAmountRequest> body)
            throws InvalidPlanException {
        if (request == null) {
            throw new InvalidPlanException("Fill in the amount.");
        }
        body.set(request);
    }
}
