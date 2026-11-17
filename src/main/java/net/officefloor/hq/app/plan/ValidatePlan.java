package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the amount is one the claim form takes for a claim that corrects nothing (a number 0.00 or more
 *  with up to two decimals, see ClaimValues); the start and end are yyyy-MM-dd, the end not before the start; every N
 *  days is a whole number, at least 1 — or, instead, a day of the month is a whole number from 1 to 31 (not both); a sheet, when
 *  given, is one of the sheet's descendants (see SettingRepository#descendants). */
public class ValidatePlan {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanRequest request,
            SettingRepository settings, Out<PlanValues> values) throws InvalidPlanException {
        if (request == null) {
            throw new InvalidPlanException("A plan is required");
        }
        ClaimValues claim;
        try {
            claim = ClaimValues.from(new ClaimRequest(request.amount(), null, "", null, request.by(), null, null));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        if (claim.amount().signum() < 0) {
            throw new InvalidPlanException("A plan's amount must be 0.00 or more");
        }
        LocalDate start = date(request.start(), "start");
        LocalDate end = date(request.end(), "end");
        if (end.isBefore(start)) {
            throw new InvalidPlanException("A plan's end must not be before its start");
        }
        String sheet = request.sheet() == null || request.sheet().trim().isEmpty() ? null : request.sheet().trim();
        if (sheet != null && !settings.descendants(sheetId.trim()).contains(sheet)) {
            throw new InvalidPlanException("A plan's sheet must be one of the sheet's descendants");
        }
        String every = request.every() == null ? "" : request.every().trim();
        String day = request.day() == null ? "" : request.day().trim();
        if (!day.isEmpty()) {
            if (!every.isEmpty()) {
                throw new InvalidPlanException("A plan runs every N days or on a day of the month, not both");
            }
            if (!day.matches("\\d{1,2}") || Integer.parseInt(day) < 1 || Integer.parseInt(day) > 31) {
                throw new InvalidPlanException("A plan's day of the month must be a whole number from 1 to 31");
            }
            values.set(new PlanValues(claim.amount(), start, null, Integer.parseInt(day), end, claim.by(), sheet));
            return;
        }
        if (!every.matches("\\d{1,6}") || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("A plan's interval must be a whole number of days, at least 1");
        }
        values.set(new PlanValues(claim.amount(), start, Integer.parseInt(every), null, end, claim.by(), sheet));
    }

    private static LocalDate date(String typed, String what) throws InvalidPlanException {
        try {
            return LocalDate.parse(typed == null ? "" : typed.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("A plan's " + what + " must be yyyy-MM-dd");
        }
    }
}
