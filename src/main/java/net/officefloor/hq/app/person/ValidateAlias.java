package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasRequest> body)
            throws InvalidSheetRequestException {
        if (request == null || request.alias() == null || request.alias().isBlank()) {
            throw new InvalidSheetRequestException("An alias is required");
        }
        body.set(request);
    }
}
