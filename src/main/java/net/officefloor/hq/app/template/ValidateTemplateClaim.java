package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the chosen template's name from the body, trimmed; refuses a blank one. */
public class ValidateTemplateClaim {
    public void validate(@RequestBody TemplateClaimRequest request, Out<TemplateClaimRequest> body) throws InvalidClaimException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        if (name.isEmpty()) {
            throw new InvalidClaimException("Choose a template.");
        }
        body.set(new TemplateClaimRequest(name));
    }
}
