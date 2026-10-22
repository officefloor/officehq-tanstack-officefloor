package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body: a signed-in user is required. */
public class ValidateGenerate {
    public void validate(@RequestBody GenerateRequest request, Out<GenerateRequest> body) throws InvalidPlanException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPlanException("Sign in before generating plans.");
        }
        body.set(new GenerateRequest(request.user().trim()));
    }
}
