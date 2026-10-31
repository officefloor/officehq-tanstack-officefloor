package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpen> body) throws InvalidSheetRequestException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the claims to open");
        }
        body.set(new BulkOpen(new LinkedHashSet<>(request.claimIds()), request.user()));
    }
}
