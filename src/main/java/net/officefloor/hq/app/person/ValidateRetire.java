package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RetireRequest request, Out<ValidRetire> body, Settings settings)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String raw = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!raw.isEmpty()) {
            try {
                date = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The retirement date is not a date");
            }
        }
        body.set(new ValidRetire(date, request.user()));
    }
}
