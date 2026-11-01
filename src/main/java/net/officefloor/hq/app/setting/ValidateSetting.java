package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.Role;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    private static final Pattern LIMIT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSettingException {
        SettingInput input = input(request);
        // A dated key may be given the date it is in force from; blank: the sheet's today.
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (Settings.dated(input.key()) && !from.isEmpty()) {
            try {
                input = input.withFrom(LocalDate.parse(from));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be a date (yyyy-mm-dd), or blank");
            }
        }
        body.set(input);
    }

    private SettingInput input(SettingRequest request) throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (!Settings.CLERK_LIMIT.equals(key) && !Settings.CAP.equals(key) && !Settings.PROOF_THRESHOLD.equals(key) && !Settings.NOTE_THRESHOLD.equals(key) && !Settings.FLOOR.equals(key) && !Settings.TERMS.equals(key)
                && !Settings.LATE_PCT.equals(key) && !Settings.POLICY_DATE.equals(key)
                && !Settings.PERIOD_NUMBERING_FROM.equals(key) && !Settings.GRACE_DAYS.equals(key) && !Settings.ENTRY_DATE.equals(key)
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeTerms(g).equals(key))
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeGrace(g).equals(key))
                && Settings.PERMISSIONS.stream().noneMatch((a) -> Settings.perm(a).equals(key))) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        // An action's permitted roles are a comma separated list of clerk and steward; blank keeps its standing rule.
        if (key.startsWith("perm-")) {
            List<String> roles = new ArrayList<>();
            for (String role : value.isEmpty() ? new String[0] : value.split(",")) {
                role = role.trim();
                if (!Role.CLERK.equals(role) && !Role.STEWARD.equals(role)) {
                    throw new InvalidSettingException("The roles allowed an action must be clerk or steward, comma separated, or blank");
                }
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
            return new SettingInput(key, String.join(",", roles));
        }
        // The policy date is a date; blank follows the sheet's today.
        if (Settings.POLICY_DATE.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The policy date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The period numbering from-date is a date; blank turns it off.
        if (Settings.PERIOD_NUMBERING_FROM.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The period numbering from-date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The entry date is a date; blank turns it off, so a new claim takes the sheet's today.
        if (Settings.ENTRY_DATE.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The entry date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The grace days are a whole number of days; blank turns the auto close off.
        if (Settings.GRACE_DAYS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // A grade's grace is a whole number of days; blank means none.
        if (Settings.GRADES.stream().anyMatch((g) -> Settings.gradeGrace(g).equals(key))) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace of a grade must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The terms are a whole number of days; blank takes the default.
        if (Settings.TERMS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, Integer.toString(Integer.parseInt(value.isEmpty() ? Settings.DEFAULT_TERMS : value)));
        }
        // A grade's own terms are a whole number of days; blank clears them, so the grade takes the sheet's terms.
        if (key.startsWith(Settings.TERMS + "-")) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms of a grade must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        if (!value.isEmpty() && !LIMIT.matcher(value).matches()) {
            throw new InvalidSettingException("The " + key + " must be 0.00 or more with up to two decimals, or blank");
        }
        // A blank floor or late percent takes its default: they always have a value.
        if (Settings.FLOOR.equals(key) && value.isEmpty()) {
            value = Settings.DEFAULT_FLOOR;
        }
        if (Settings.LATE_PCT.equals(key) && value.isEmpty()) {
            value = Settings.DEFAULT_LATE_PCT;
        }
        return new SettingInput(key,
                value.isEmpty() ? "" : new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }
}
