package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassignRequest> body) {
        List<String> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        String personId = request == null || request.personId() == null ? "" : request.personId().trim();
        body.set(new BulkReassignRequest(ids, personId));
    }
}
