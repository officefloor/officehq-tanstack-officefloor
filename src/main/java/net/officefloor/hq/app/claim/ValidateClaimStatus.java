package net.officefloor.hq.app.claim;

import java.util.Locale;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusChange> body)
            throws InvalidSheetRequestException {
        String action = request == null || request.action() == null ? "" : request.action().trim();
        try {
            body.set(new ClaimStatusChange(ClaimAction.valueOf(action.toUpperCase(Locale.ROOT)), request.user()));
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetRequestException("Not a claim action (open, settle or void): " + action);
        }
    }
}
