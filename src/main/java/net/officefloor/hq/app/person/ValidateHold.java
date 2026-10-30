package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldRequest> body) {
        body.set(request);
    }
}
