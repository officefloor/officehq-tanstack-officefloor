package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body) {
        body.set(request == null ? new PatronRequest(null, null) : request);
    }
}
