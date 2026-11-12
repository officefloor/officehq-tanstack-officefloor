package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.user());
        String text = request == null || request.today() == null ? "" : request.today().trim();
        body.set(request);
        try {
            today.set(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
