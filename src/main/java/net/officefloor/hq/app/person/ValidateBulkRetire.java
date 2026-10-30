package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk retire the row's retire would refuse for every person: a date that is not a date.
 *  A viewer changes nothing: their bulk retire retires no one. */
public class ValidateBulkRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkRetireRequest request,
            Out<ValidBulkRetire> body, Settings settings) throws InvalidPersonException {
        boolean viewer = false;
        try {
            settings.refuseViewer(sheetId, request.user());
        } catch (ViewerRefusedException e) {
            viewer = true;
        }
        String raw = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!raw.isEmpty()) {
            try {
                date = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The retirement date is not a date");
            }
        }
        body.set(new ValidBulkRetire(viewer || request.personIds() == null ? List.of() : request.personIds(), date, request.user()));
    }
}
