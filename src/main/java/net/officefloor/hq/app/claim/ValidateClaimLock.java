package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimLock {
    public void validate(@RequestBody ClaimLockRequest request, Out<ClaimLock> body) throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who opens the claim's editor must be named");
        body.set(new ClaimLock(user));
    }
}
