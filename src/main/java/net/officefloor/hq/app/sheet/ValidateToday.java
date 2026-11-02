package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Reads the requested today, refusing a bad date or a role the permission matrix does not let change it. */
public class ValidateToday {
    public void validate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody TodayRequest request, Settings settings, Roles roles, Out<LocalDate> today)
            throws InvalidTodayException {
        String raw = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException("Not a valid date: " + raw);
        }
        if (!settings.permits(sheet, "today", roles.of(sheet, user))) {
            throw new InvalidTodayException("Your role may not change the sheet's today");
        }
    }
}
