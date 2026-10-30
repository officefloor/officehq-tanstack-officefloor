package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerate {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request,
            Out<ValidGenerate> body, Settings settings, Sheets sheets)
            throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        LocalDate from = date(sheetId, request.from(), "from", sheets);
        LocalDate to = date(sheetId, request.to(), "to", sheets);
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date");
        }
        body.set(new ValidGenerate(from, to, request.user()));
    }

    private static LocalDate date(String sheetId, String raw, String which, Sheets sheets)
            throws InvalidPeriodException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return sheets.today(sheetId);
        }
        try {
            return LocalDate.parse(value, DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (YYYY-MM-DD)");
        }
    }
}
