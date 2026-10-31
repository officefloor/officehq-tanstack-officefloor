package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchRequest> body) throws InvalidSheetRequestException {
        if (request == null || request.user() == null || request.user().isBlank())
            throw new InvalidSheetRequestException("A watch needs the signed-in user");
        body.set(new WatchRequest(request.user().trim()));
    }
}
