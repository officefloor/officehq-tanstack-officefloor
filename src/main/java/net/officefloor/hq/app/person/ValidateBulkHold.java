package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk hold the row's hold would refuse for every person: a date that is not a date.
 *  A viewer changes nothing: their bulk hold holds no one. */
public class ValidateBulkHold {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkHoldRequest request,
            Out<ValidBulkHold> body, Settings settings) throws InvalidPersonException {
        boolean viewer = false;
        try {
            settings.refuseViewer(sheetId, request.user());
        } catch (ViewerRefusedException e) {
            viewer = true;
        }
        body.set(new ValidBulkHold(viewer || request.personIds() == null ? List.of() : request.personIds(),
                date(request.from(), "from"), date(request.to(), "to"), request.user()));
    }

    private static LocalDate date(String value, String which) throws InvalidPersonException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold's " + which + "-date is not a date");
        }
    }
}
