package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the generate-periods form from the body; its dates are checked once the sheet (and its today) is loaded. */
public class ValidateGeneratePeriods {
    public void validate(@RequestBody GeneratePeriodsRequest request, Out<GeneratePeriodsRequest> body) {
        body.set(request == null ? new GeneratePeriodsRequest(null, null) : request);
    }
}
