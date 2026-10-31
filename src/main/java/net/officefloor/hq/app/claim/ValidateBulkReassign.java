package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassign> body) throws InvalidSheetRequestException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the claims to reassign");
        }
        if (request.toPersonId() == null) {
            throw new InvalidSheetRequestException("Choose the person to reassign the claims to");
        }
        body.set(new BulkReassign(new LinkedHashSet<>(request.claimIds()), request.toPersonId(), request.user()));
    }
}
