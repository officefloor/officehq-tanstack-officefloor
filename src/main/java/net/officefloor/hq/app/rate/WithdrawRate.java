package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class WithdrawRate {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // Only a steward withdraws a rate. The newest version of the grade and from-date's rate is removed and the one
    // before it applies again; with none before it the rate is gone. A grade and from-date with no rate is refused.
    public void withdraw(@PathVariable("sheetId") String sheetId, @PathVariable("grade") String grade,
            @RequestBody RateWithdrawRequest body, Rates rates, Settings settings, ServerHttpConnection connection,
            Out<WithdrawnRate> withdrawnOut) throws InvalidRateException {
        String sheet = sheetId.trim();
        String g = grade == null ? "" : grade.trim();
        if (!Settings.STEWARD.equals(settings.role(sheet, Audit.user(connection).trim()))) {
            throw new InvalidRateException("Only a steward may withdraw a rate.");
        }
        String typedFrom = body == null || body.from() == null ? "" : body.from().trim();
        LocalDate from = null;
        if (!typedFrom.isEmpty() && !typedFrom.equals("start")) {
            try {
                from = LocalDate.parse(typedFrom, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date (yyyy-mm-dd).");
            }
        }
        WithdrawnRate withdrawn = rates.withdraw(sheet, g, from);
        if (withdrawn == null) {
            throw new InvalidRateException("There is no such rate to withdraw.");
        }
        withdrawnOut.set(withdrawn);
    }
}
