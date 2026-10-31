package net.officefloor.hq.app.person;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkRetire {
    public void validate(@RequestBody BulkRetireRequest request, Out<BulkRetire> body) throws InvalidSheetRequestException {
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the people to retire");
        }
        // Checked by the row's retire rules, so a date the row refuses is refused here too.
        Retirement checked = ValidateRetire.checked(new RetireRequest(request.from(), request.user()));
        body.set(new BulkRetire(new LinkedHashSet<>(request.personIds()), checked));
    }
}
