package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user. */
public class ValidatePlanPause {
    public void validate(@RequestBody PlanPauseRequest request, Out<PlanPauseRequest> body) throws InvalidPlanException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPlanException("Sign in before pausing or resuming plans.");
        }
        body.set(new PlanPauseRequest(request.user().trim()));
    }
}
