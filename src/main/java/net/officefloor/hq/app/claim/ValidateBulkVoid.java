package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoid> body) throws InvalidSheetRequestException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the claims to void");
        }
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        body.set(new BulkVoid(new LinkedHashSet<>(request.claimIds()), request.user(), reason));
    }
}
