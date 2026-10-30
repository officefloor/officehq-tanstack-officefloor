package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateUnlock {
    public void validate(@RequestBody LockRequest request, Out<LockRequest> body) {
        body.set(request);
    }
}
