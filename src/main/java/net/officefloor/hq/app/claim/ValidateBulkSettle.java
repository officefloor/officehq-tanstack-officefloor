package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkSettle {
    public void validate(@RequestBody BulkSettleRequest request, Out<BulkSettle> body) throws InvalidSheetRequestException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the claims to settle");
        }
        body.set(new BulkSettle(new LinkedHashSet<>(request.claimIds()), request.user()));
    }
}
