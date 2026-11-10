package net.officefloor.hq.app.delegation;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateDelegation {
    public void validate(@RequestBody DelegationRequest request, Out<DelegationInput> body)
            throws InvalidDelegationException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidDelegationException("A user name is required");
        String from = request.from() == null ? "" : request.from().trim();
        String to = request.to() == null ? "" : request.to().trim();
        body.set(new DelegationInput(user, from, to));
    }
}
