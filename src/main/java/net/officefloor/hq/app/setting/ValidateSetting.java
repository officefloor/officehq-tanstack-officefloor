package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    public void validate(@RequestBody SettingRequest request, Out<SettingRequest> body)
            throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (SettingRepository.CLERK_LIMIT.equals(key)) {
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The clerk limit is blank or 0.00 or more with up to two decimals");
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
            body.set(new SettingRequest(key, String.valueOf(Integer.parseInt(value))));
            return;
        } else if (key.startsWith(SettingRepository.TERMS_OF_GRADE)
                && GRADES.contains(key.substring(SettingRepository.TERMS_OF_GRADE.length()))) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("A grade's terms are blank or a whole number of days, 0 or more");
            }
            body.set(new SettingRequest(key, value.isEmpty() ? null : String.valueOf(Integer.parseInt(value))));
            return;
        } else if (SettingRepository.POLICY_DATE.equals(key)
                || SettingRepository.PERIOD_NUMBERING_FROM.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The " + key + " setting is blank or a date (yyyy-MM-dd)");
                }
            }
            body.set(new SettingRequest(key, value.isEmpty() ? null : value));
            return;
        } else {
            throw new InvalidSettingException("Unknown setting: " + key);
        }
        body.set(new SettingRequest(key,
                value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString()));
    }
}
