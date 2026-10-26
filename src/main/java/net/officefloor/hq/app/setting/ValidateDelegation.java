package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateDelegation {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // Only a steward (by the role the sheet gives them, not one delegated to them) delegates. The user name must not be
    // blank; a blank from-date is the sheet's today, a blank to-date the from-date; a to-date before the from-date is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DelegateRequest body,
            ServerHttpConnection connection, Settings settings, Sheets sheets, Out<Delegation> delegation)
            throws InvalidSettingException {
        String id = sheetId.trim();
        String by = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.givenRole(id, by))) {
            throw new InvalidSettingException("Only a steward can delegate the steward role.");
        }
        String user = body == null || body.user() == null ? "" : body.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSettingException("A user name is needed.");
        }
        LocalDate from = parse(body.from());
        if (from == null) {
            from = sheets.today(id);
        }
        LocalDate to = parse(body.to());
        if (to == null) {
            to = from;
        }
        if (to.isBefore(from)) {
            throw new InvalidSettingException("The to-date is before the from-date.");
        }
        delegation.set(new Delegation(id, user, from, to, by));
    }

    private static LocalDate parse(String value) throws InvalidSettingException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw, ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}
