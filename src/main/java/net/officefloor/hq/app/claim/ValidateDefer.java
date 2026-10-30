package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateDefer {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DeferRequest request, Out<ValidDefer> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String until = request.until() == null ? "" : request.until().trim();
        try {
            body.set(new ValidDefer(request.user(), LocalDate.parse(until)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A deferral needs a date to defer the claim until");
        }
    }
}
