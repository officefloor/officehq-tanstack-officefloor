package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern LIMIT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    /** A whole number of days, 0 or more. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSheetException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        boolean floor = SettingStore.FLOOR.equals(key);
        boolean terms = SettingStore.TERMS.equals(key);
        boolean gradeTerms = key.startsWith(SettingStore.TERMS_PREFIX)
                && SettingStore.GRADES.contains(key.substring(SettingStore.TERMS_PREFIX.length()));
        boolean late = SettingStore.LATE_PCT.equals(key);
        boolean numbering = SettingStore.PERIOD_NUMBERING_FROM.equals(key);
        boolean dated = SettingStore.POLICY_DATE.equals(key) || numbering;
        if (!SettingStore.CLERK_LIMIT.equals(key) && !floor && !terms && !gradeTerms && !late && !dated) {
            throw new InvalidSheetException("Not a setting: " + key);
        }
        String value = request.value() == null ? "" : request.value().trim();
        if (dated) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, request.user()));
                return;
            }
            try {
                body.set(new SettingInput(key, LocalDate.parse(value).toString(), request.user()));
            } catch (DateTimeParseException e) {
                throw new InvalidSheetException((numbering ? "The period numbering from" : "The policy date")
                        + " must be a date (YYYY-MM-DD), or blank");
            }
            return;
        }
        if (gradeTerms) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The grade's terms must be a whole number of days, 0 or more, or blank");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), request.user()));
            return;
        }
        if (terms) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, String.valueOf(SettingStore.DEFAULT_TERMS), request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The terms must be a whole number of days, 0 or more");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), request.user()));
            return;
        }
        if (value.isEmpty() && floor) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_FLOOR.toPlainString(), request.user()));
            return;
        }
        if (value.isEmpty() && late) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_LATE_PCT.toPlainString(), request.user()));
            return;
        }
        if (value.isEmpty()) {
            body.set(new SettingInput(key, null, request.user()));
            return;
        }
        if (!LIMIT.matcher(value).matches()) {
            throw new InvalidSheetException(floor ? "The floor must be 0.00 or more with up to two decimals"
                    : late ? "The late percent must be 0.00 or more with up to two decimals"
                    : "The clerk limit must be 0.00 or more with up to two decimals, or blank");
        }
        body.set(new SettingInput(key, new BigDecimal(value).setScale(2).toPlainString(), request.user()));
    }
}
