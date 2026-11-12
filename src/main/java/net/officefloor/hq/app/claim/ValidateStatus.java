package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatus {
    public void validate(@RequestBody StatusRequest request, Out<StatusInput> body) throws InvalidSheetException {
        String status = request == null || request.status() == null ? "" : request.status().trim();
        try {
            body.set(new StatusInput(ClaimStatus.valueOf(status), request.user()));
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetException("Not a claim status: " + status);
        }
    }
}
