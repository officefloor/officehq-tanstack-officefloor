package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClose {
    public void validate(@RequestBody CloseRequest request, Out<CloseRequest> body) {
        body.set(request == null ? new CloseRequest(null) : request);
    }
}
