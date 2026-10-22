package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, at least one selected claim and a person
 *  to move them to. */
public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassignInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        if (request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select the claims to move.");
        }
        long personId;
        try {
            personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
        } catch (NumberFormatException e) {
            throw new InvalidClaimException("Choose the person to move the claims to.");
        }
        body.set(new BulkReassignInput(request.user().trim(), new LinkedHashSet<>(request.claimIds()), personId));
    }
}
