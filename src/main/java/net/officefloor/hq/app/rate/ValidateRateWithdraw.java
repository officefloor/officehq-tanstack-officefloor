package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward may withdraw a rate, and only one the sheet has for that grade and from-date. */
public class ValidateRateWithdraw {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateWithdrawRequest request,
            Rates rates, Settings settings, Out<RateWithdrawRequest> body, Out<RateWithdrawal> withdrawal,
            Out<Actor> actor) throws InvalidSheetRequestException {
        if (request == null || request.grade() == null) {
            throw new InvalidSheetRequestException("A rate's grade is needed");
        }
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward may withdraw a rate");
        }
        LocalDate from = date(request.from());
        boolean exists = rates.onSheet(sheetId).stream()
                .anyMatch(r -> r.grade().equals(request.grade()) && Objects.equals(r.from(), from));
        if (!exists) {
            throw new InvalidSheetRequestException("There is no such rate to withdraw");
        }
        actor.set(new Actor(request.user()));
        body.set(request);
        withdrawal.set(new RateWithdrawal(sheetId, request.grade(), from));
    }

    /** A yyyy-mm-dd date, or null when blank. */
    private static LocalDate date(String value) throws InvalidSheetRequestException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty() || raw.equals("start")) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}
