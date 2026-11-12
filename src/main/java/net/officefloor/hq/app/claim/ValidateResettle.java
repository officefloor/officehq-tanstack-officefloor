package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateResettle {
    public void validate(@RequestBody ResettleRequest request, Out<ResettleInput> body) {
        body.set(new ResettleInput(request == null ? null : request.user()));
    }
}
