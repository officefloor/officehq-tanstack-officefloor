package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignInput> body) throws InvalidSheetException {
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidSheetException("Choose the person to move the claim to");
        }
        try {
            body.set(new ReassignInput(Long.parseLong(to), request.user()));
        } catch (NumberFormatException e) {
            throw new InvalidSheetException("No such person: " + to);
        }
    }
}
