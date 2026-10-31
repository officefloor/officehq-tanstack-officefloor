package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body)
            throws InvalidSheetRequestException {
        if (request == null || request.patronId() == null) {
            throw new InvalidSheetRequestException("Choose the person to name as patron");
        }
        body.set(request);
    }
}
