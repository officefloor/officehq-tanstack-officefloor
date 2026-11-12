package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatus {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody StatusRequest request, Out<StatusInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.user());
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
