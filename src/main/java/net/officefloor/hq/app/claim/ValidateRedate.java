package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateRedate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RedateRequest request, Out<ValidRedate> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String date = request.date() == null ? "" : request.date().trim();
        try {
            body.set(new ValidRedate(request.user(), LocalDate.parse(date)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A re-dating needs the claim's new date");
        }
    }
}
