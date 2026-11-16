package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who reminds the claim (a blank name is refused). */
public class ValidateRemind {
    public void validate(@RequestBody RemindRequest request, Out<ClaimReminder> reminder)
            throws InvalidClaimStatusException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidClaimStatusException("Who reminds the claim is required");
        }
        reminder.set(new ClaimReminder(by));
    }
}
