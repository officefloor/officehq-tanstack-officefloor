package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateToday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body, Settings settings)
            throws InvalidTodayException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        body.set(request); // who changes it, for the audit
        String raw = request.today() == null ? "" : request.today().trim();
        if (raw.isEmpty()) {
            today.set(LocalDate.now()); // blank takes the default: the real date
            return;
        }
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException(raw);
        }
    }
}
