package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpenRequest> body) throws InvalidClaimException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select at least one claim to open.");
        }
        body.set(new BulkOpenRequest(List.copyOf(request.claimIds())));
    }
}
