package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidatePatron {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PatronRequest request, Out<PatronRequest> body, Settings settings)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (request.patronId() == null || request.patronId().isBlank()) {
            throw new InvalidPersonException("No patron chosen");
        }
        body.set(request);
    }
}
