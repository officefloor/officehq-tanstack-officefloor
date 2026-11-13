package net.officefloor.hq.app.plan;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the plan the row's form submits; a body with none is refused (400). */
public class ValidatePlanRequest {
    public void validate(@RequestBody @Valid PlanRequest request, Out<PlanRequest> body) throws InvalidPlanException {
        if (request == null) {
            throw new InvalidPlanException("Fill in the plan.");
        }
        body.set(request);
    }
}
