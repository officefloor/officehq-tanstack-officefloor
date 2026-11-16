package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who closes the period. */
public class ValidatePeriodClose {
    public void validate(@RequestBody PeriodCloseRequest request, Out<PeriodCloseRequest> body) {
        body.set(new PeriodCloseRequest(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
