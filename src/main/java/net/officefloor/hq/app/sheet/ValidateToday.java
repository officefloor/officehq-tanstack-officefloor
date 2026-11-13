package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body, RoleStore roles, SettingStore settings) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        Role role = roles.find(sheetId.trim(), request == null || request.user() == null ? "" : request.user().trim());
        if (!settings.allows(sheetId.trim(), "today", role)) {
            throw new InvalidSheetException("A " + role.shown() + " may not change the sheet's today");
        }
        String text = request == null || request.today() == null ? "" : request.today().trim();
        body.set(request);
        try {
            today.set(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
