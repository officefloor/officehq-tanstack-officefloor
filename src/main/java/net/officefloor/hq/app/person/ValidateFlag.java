package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** Only a steward flags a person, with a reason that is not blank and an until date. */
public class ValidateFlag {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody FlagRequest request, Out<ValidFlag> body,
            Settings settings) throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new ViewerRefusedException("Only a steward can flag a person");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason");
        }
        String raw = request.until() == null ? "" : request.until().trim();
        LocalDate until;
        try {
            until = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The flag's until date is not a date");
        }
        body.set(new ValidFlag(reason, until, request.user()));
    }
}
