package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatus {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody StatusRequest request, Out<StatusInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
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
        LocalDate on = null;
        if (to == ClaimStatus.SETTLED && request.on() != null && !request.on().isBlank()) {
            try {
                on = LocalDate.parse(request.on().trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSheetException("Not a date: " + request.on().trim());
            }
        }
        body.set(new StatusInput(to, request.user(), review, reason, on));
    }
}
