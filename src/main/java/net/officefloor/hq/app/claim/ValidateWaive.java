package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who waives the claim's late charge. */
public class ValidateWaive {
    public void validate(@RequestBody WaiveRequest request, Out<ClaimWaiver> waiver) {
        waiver.set(new ClaimWaiver(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
