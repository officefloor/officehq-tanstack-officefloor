package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A void's reason, kept trimmed; null when none was chosen. Whether it is needed is the void's rule. */
public class ValidateVoid {
    public void validate(@RequestBody VoidRequest request, Out<VoidRequest> body) {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        body.set(new VoidRequest(reason.isEmpty() ? null : reason));
    }
}
