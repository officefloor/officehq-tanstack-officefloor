package net.officefloor.hq.app.person;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkHold {
    public void validate(@RequestBody BulkHoldRequest request, Out<BulkHold> body) throws InvalidSheetRequestException {
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the people to hold");
        }
        // Checked by the row's hold rules, so dates the row refuses are refused here too.
        HoldChange checked = ValidateHold.checked(new HoldRequest(request.from(), request.to(), request.user()));
        body.set(new BulkHold(new LinkedHashSet<>(request.personIds()), checked));
    }
}
