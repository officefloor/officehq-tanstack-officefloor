package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the key is clerk-limit, its value blank (no limit) or an amount 0.00 or more with up to two
 *  decimals; or floor or late-pct, its value such an amount (not blank), all kept with two decimals; or terms, its value a
 *  whole number of days 0 or more (not blank), kept without leading zeros, and so is terms-A, terms-B or terms-C (a
 *  grade's own terms); or policy-date, its value a date yyyy-MM-dd or blank (the sheet's today); or
 *  period-numbering-from, its value a date yyyy-MM-dd or blank (off); or cap, its value blank (none) or an amount
 *  like the clerk limit. */
public class ValidateSetting {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("key") String key, @RequestBody SettingRequest request, Out<SettingValues> values)
            throws InvalidSettingException {
        String k = key == null ? "" : key.trim();
        boolean floor = SettingRepository.FLOOR.equals(k);
        boolean terms = SettingRepository.TERMS.equals(k) || (k.startsWith(SettingRepository.TERMS_PREFIX)
                && GRADES.contains(k.substring(SettingRepository.TERMS_PREFIX.length())));
        boolean latePct = SettingRepository.LATE_PCT.equals(k);
        boolean policyDate = SettingRepository.POLICY_DATE.equals(k);
        boolean numberingFrom = SettingRepository.PERIOD_NUMBERING_FROM.equals(k);
        boolean cap = SettingRepository.CAP.equals(k);
        if (!SettingRepository.CLERK_LIMIT.equals(k) && !cap && !floor && !terms && !latePct && !policyDate && !numberingFrom) {
            throw new InvalidSettingException("Unknown setting " + k);
        }
        String value = request == null || request.value() == null ? "" : request.value().trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (policyDate || numberingFrom) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, by));
                return;
            }
            try {
                values.set(new SettingValues(k, LocalDate.parse(value).toString(), by));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException((policyDate ? "The policy date" : "The period numbering from-date")
                        + " must be yyyy-MM-dd, or blank");
            }
            return;
        }
        if (terms) {
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more");
            }
            values.set(new SettingValues(k, String.valueOf(Integer.parseInt(value)), by));
            return;
        }
        if (latePct && !AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The late percent must be a percent with up to two decimals");
        }
        if (floor && value.isEmpty()) {
            throw new InvalidSettingException("The floor must be an amount with up to two decimals");
        }
        if (!value.isEmpty() && !AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException(floor ? "The floor must be an amount with up to two decimals"
                    : cap ? "The cap must be an amount with up to two decimals, or blank"
                    : "The clerk limit must be an amount with up to two decimals, or blank");
        }
        values.set(new SettingValues(k, value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(),
                by));
    }
}
