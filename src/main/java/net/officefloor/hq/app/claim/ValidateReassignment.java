package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the move from the body, refusing one that names no person. */
public class ValidateReassignment {
    public void validate(@RequestBody ReassignRequest request, Out<Reassignment> reassignmentOut) throws InvalidClaimException {
        if (request == null || request.personId() == null) {
            throw new InvalidClaimException("A claim must be moved to a person.");
        }
        reassignmentOut.set(new Reassignment(request.personId()));
    }
}
