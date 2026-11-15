package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    /** 0 or more, with up to two decimals. */
    private static final Pattern MONEY = Pattern.compile("(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");

    /** A whole number of days, 0 or more. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,6}");

    /** The grades that may have their own terms. */
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** The roles a permission may name. */
    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD);

    public void validate(@RequestBody SettingRequest request, Out<SettingRequest> body)
            throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        String from = request == null || request.from() == null || !SettingRepository.isDated(key)
                ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("A setting's from-date is blank or a date (yyyy-MM-dd)");
            }
        }
        String given = from.isEmpty() ? null : from;
        if (SettingRepository.CLERK_LIMIT.equals(key)) {
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The clerk limit is blank or 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.PROOF_THRESHOLD.equals(key)) {
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The proof threshold is blank or 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.NOTE_THRESHOLD.equals(key)) {
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The note threshold is blank or 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.CAP.equals(key)) {
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The cap is blank or 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.FLOOR.equals(key)) {
            if (!MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The floor is 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.LATE_PCT.equals(key)) {
            if (!MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The late percent is 0.00 or more with up to two decimals");
            }
        } else if (SettingRepository.TERMS.equals(key)) {
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms are a whole number of days, 0 or more");
            }
            body.set(new SettingRequest(key, String.valueOf(Integer.parseInt(value)), given));
            return;
        } else if (key.startsWith(SettingRepository.TERMS_OF_GRADE)
                && GRADES.contains(key.substring(SettingRepository.TERMS_OF_GRADE.length()))) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("A grade's terms are blank or a whole number of days, 0 or more");
            }
            body.set(new SettingRequest(key, value.isEmpty() ? null : String.valueOf(Integer.parseInt(value)), given));
            return;
        } else if (SettingRepository.GRACE_DAYS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace days are blank or a whole number of days, 0 or more");
            }
            body.set(new SettingRequest(key, value.isEmpty() ? null : String.valueOf(Integer.parseInt(value)), null));
            return;
        } else if (SettingRepository.POLICY_DATE.equals(key)
                || SettingRepository.PERIOD_NUMBERING_FROM.equals(key)
                || SettingRepository.ENTRY_DATE.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The " + key + " setting is blank or a date (yyyy-MM-dd)");
                }
            }
            body.set(new SettingRequest(key, value.isEmpty() ? null : value, null));
            return;
        } else if (Permissions.isKey(key)) {
            List<String> roles = Permissions.roles(value);
            if (!roles.stream().allMatch(ROLES::contains)) {
                throw new InvalidSettingException("A permission is blank or a comma separated list of clerk and steward");
            }
            body.set(new SettingRequest(key, roles.isEmpty() ? null : String.join(",", roles), null));
            return;
        } else {
            throw new InvalidSettingException("Unknown setting: " + key);
        }
        body.set(new SettingRequest(key,
                value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(), given));
    }
}
