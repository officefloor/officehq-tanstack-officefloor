package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings form from the body; refuses a clerk limit or floor that is negative or has more than two
 *  decimals, and terms that are not a whole number of days, and a late percent that is negative or has more than two decimals, and a policy date or period numbering from that is not a calendar date
 *  (YYYY-MM-DD). A blank clerk limit means no limit; a blank floor is
 *  1.00; blank terms are 14; a blank late percent is 0.00; a blank policy date is the sheet's today; a blank period numbering from is off; a blank cap is none (else an amount like the floor). A grade's terms must be a whole number of days, or blank (the grade uses the sheet's terms). A key left out is not changed.
 *  A from-date must be a calendar date (YYYY-MM-DD), or blank (the sheet's today). */
public class ValidateSettings {

    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingsException {
        List<SettingsInput.Change> changes = new ArrayList<>();
        if (request != null) {
            amount(changes, Sheet.CLERK_LIMIT, request.clerkLimit(), null, request.clerkLimitFrom(),
                    "The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.FLOOR, request.floor(), Sheet.DEFAULT_FLOOR, request.floorFrom(),
                    "The floor must be an amount, 0.00 or more, with up to two decimals, or blank.");
            days(changes, Sheet.TERMS, request.terms(), Sheet.DEFAULT_TERMS, request.termsFrom(),
                    "The terms must be a whole number of days, 0 or more, or blank.");
            amount(changes, Sheet.LATE_PCT, request.latePct(), Sheet.DEFAULT_LATE_PCT, request.latePctFrom(),
                    "The late percent must be a percent, 0.00 or more, with up to two decimals, or blank.");
            date(changes, "policy-date", request.policyDate(), "The policy date must be a date (YYYY-MM-DD), or blank.");
            date(changes, "period-numbering-from", request.periodNumberingFrom(),
                    "The period numbering from must be a date (YYYY-MM-DD), or blank.");
            days(changes, Sheet.TERMS + "-A", request.termsA(), null, request.termsAFrom(),
                    "The terms of grade A must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.TERMS + "-B", request.termsB(), null, request.termsBFrom(),
                    "The terms of grade B must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.TERMS + "-C", request.termsC(), null, request.termsCFrom(),
                    "The terms of grade C must be a whole number of days, 0 or more, or blank.");
            amount(changes, Sheet.CAP, request.cap(), null, request.capFrom(),
                    "The cap must be an amount, 0.00 or more, with up to two decimals, or blank.");
        }
        body.set(new SettingsInput(changes));
    }

    /** Adds an amount setting when the form sets it: blank is the given default (null: none). */
    private static void amount(List<SettingsInput.Change> changes, String key, String value, BigDecimal blank,
            String from, String message) throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        BigDecimal amount = blank;
        if (!value.trim().isEmpty()) {
            if (!AMOUNT.matcher(value.trim()).matches()) {
                throw new InvalidSettingsException(message);
            }
            amount = new BigDecimal(value.trim());
        }
        changes.add(new SettingsInput.Change(key, amount == null ? null : amount.setScale(2).toPlainString(), true,
                from(key, from)));
    }

    /** Adds a whole-days setting when the form sets it: blank is the given default (null: none). */
    private static void days(List<SettingsInput.Change> changes, String key, String value, Integer blank,
            String from, String message) throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        Integer days = blank;
        if (!value.trim().isEmpty()) {
            if (!DAYS.matcher(value.trim()).matches()) {
                throw new InvalidSettingsException(message);
            }
            days = Integer.parseInt(value.trim());
        }
        changes.add(new SettingsInput.Change(key, days == null ? null : String.valueOf(days), true, from(key, from)));
    }

    /** Adds an undated date setting when the form sets it: blank is null. */
    private static void date(List<SettingsInput.Change> changes, String key, String value, String message)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        LocalDate date = null;
        if (!value.trim().isEmpty()) {
            try {
                date = LocalDate.parse(value.trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSettingsException(message);
            }
        }
        changes.add(new SettingsInput.Change(key, date == null ? null : date.toString(), false, null));
    }

    /** A setting's from-date: null when blank or left out (the sheet's today). */
    private static LocalDate from(String key, String from) throws InvalidSettingsException {
        if (from == null || from.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(from.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSettingsException("The from-date of the " + key + " must be a date (YYYY-MM-DD), or blank.");
        }
    }
}
