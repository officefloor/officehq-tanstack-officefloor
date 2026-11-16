package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the selected claim ids (each once; a missing list is none), the person they move to (a move needs
 *  one) and who moves them. */
public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassignRequest> body)
            throws InvalidClaimStatusException {
        if (request == null || request.personId() == null) {
            throw new InvalidClaimStatusException("A move needs the person the claims move to");
        }
        List<Long> ids = request.claimIds() == null ? List.of()
                : request.claimIds().stream().filter(Objects::nonNull).distinct().toList();
        String by = request.by() == null ? "" : request.by().trim();
        body.set(new BulkReassignRequest(ids, request.personId(), by));
    }
}
