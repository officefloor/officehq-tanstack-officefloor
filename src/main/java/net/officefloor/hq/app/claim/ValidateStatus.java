package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatus {
    public void validate(@RequestBody StatusRequest request, Out<StatusInput> body) throws InvalidSheetException {
        String status = request == null || request.status() == null ? "" : request.status().trim();
        ClaimStatus to;
        try {
            to = ClaimStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetException("Not a claim status: " + status);
        }
        boolean review = Boolean.TRUE.equals(request.review());
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (review && to != ClaimStatus.SETTLED && to != ClaimStatus.OPEN) {
            throw new InvalidSheetException("A review approves or rejects a claim");
        }
        if (review && to == ClaimStatus.OPEN && reason.isEmpty()) {
            throw new InvalidSheetException("A rejection needs a reason");
        }
        body.set(new StatusInput(to, request.user(), review, reason));
    }
}
