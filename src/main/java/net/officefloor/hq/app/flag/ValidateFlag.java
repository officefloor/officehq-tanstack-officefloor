package net.officefloor.hq.app.flag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateFlag {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // Only a steward (by the role they act in on the sheet's today) flags, and only with a reason; a blank until-date
    // is the sheet's today.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody FlagRequest body, Settings settings,
            Sheets sheets, ServerHttpConnection connection, Out<Flag> flag) throws InvalidPersonException {
        String sheet = sheetId.trim();
        if (!Settings.STEWARD.equals(settings.role(sheet, Audit.user(connection).trim()))) {
            throw new InvalidPersonException("Only a steward can flag a person.");
        }
        String reason = body == null || body.reason() == null ? "" : body.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("Give a reason to flag the person.");
        }
        String raw = body.until() == null ? "" : body.until().trim();
        LocalDate until;
        try {
            until = raw.isEmpty() ? sheets.today(sheet) : LocalDate.parse(raw, ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a date (yyyy-mm-dd): " + raw);
        }
        flag.set(new Flag(0, reason, until));
    }
}
