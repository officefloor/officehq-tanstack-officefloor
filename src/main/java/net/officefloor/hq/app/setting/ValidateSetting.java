package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.role.RoleRepository;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the key is clerk-limit, its value blank (no limit) or an amount 0.00 or more with up to two
 *  decimals; or floor or late-pct, its value such an amount (not blank), all kept with two decimals; or terms, its value a
 *  whole number of days 0 or more (not blank), kept without leading zeros, and so is terms-A, terms-B or terms-C (a
 *  grade's own terms); or grace-days, its value blank (off) or a whole number of days 0 or more (undated); or policy-date, its value a date yyyy-MM-dd or blank (the sheet's today); or
 *  period-numbering-from or entry-date, its value a date yyyy-MM-dd or blank (off); or cap, proof-threshold or note-threshold, its value blank (none) or
 *  an amount like the clerk limit; or perm-open, perm-settle, perm-void, perm-reassign, perm-close or perm-reopen, its value
 *  blank (the standing rule) or roles (clerk, steward) separated by commas, kept without spaces or repeats. Every key but policy-date, period-numbering-from, entry-date, grace-days and the permissions is dated: its from-date is blank (the
 *  sheet's today) or a date yyyy-MM-dd. */
public class ValidateSetting {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Set<String> ROLES = Set.of(RoleRepository.CLERK, RoleRepository.STEWARD);

    public void validate(@PathVariable("key") String key, @RequestBody SettingRequest request, Out<SettingValues> values)
            throws InvalidSettingException {
        String k = key == null ? "" : key.trim();
        boolean floor = SettingRepository.FLOOR.equals(k);
        boolean terms = SettingRepository.TERMS.equals(k) || (k.startsWith(SettingRepository.TERMS_PREFIX)
                && GRADES.contains(k.substring(SettingRepository.TERMS_PREFIX.length())));
        boolean latePct = SettingRepository.LATE_PCT.equals(k);
        boolean policyDate = SettingRepository.POLICY_DATE.equals(k);
        boolean numberingFrom = SettingRepository.PERIOD_NUMBERING_FROM.equals(k);
        boolean graceDays = SettingRepository.GRACE_DAYS.equals(k);
        boolean entryDate = SettingRepository.ENTRY_DATE.equals(k);
        boolean cap = SettingRepository.CAP.equals(k);
        boolean proofThreshold = SettingRepository.PROOF_THRESHOLD.equals(k);
        boolean noteThreshold = SettingRepository.NOTE_THRESHOLD.equals(k);
        boolean perm = k.startsWith(SettingRepository.PERM_PREFIX)
                && SettingRepository.PERMISSIONS.containsKey(k.substring(SettingRepository.PERM_PREFIX.length()));
        if (!perm && !SettingRepository.CLERK_LIMIT.equals(k) && !cap && !proofThreshold && !noteThreshold && !floor && !terms && !latePct && !policyDate && !numberingFrom && !graceDays && !entryDate) {
            throw new InvalidSettingException("Unknown setting " + k);
        }
        String value = request == null || request.value() == null ? "" : request.value().trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String fromText = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!policyDate && !numberingFrom && !entryDate && !graceDays && !fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be yyyy-MM-dd, or blank");
            }
        }
        if (perm) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            List<String> roles = new ArrayList<>();
            for (String role : value.split(",", -1)) {
                String r = role.trim();
                if (!ROLES.contains(r)) {
                    throw new InvalidSettingException("A permission must be roles (clerk, steward) separated by commas, or blank");
                }
                if (!roles.contains(r)) {
                    roles.add(r);
                }
            }
            values.set(new SettingValues(k, String.join(",", roles), null, false, by));
            return;
        }
        if (policyDate || numberingFrom || entryDate) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            try {
                values.set(new SettingValues(k, LocalDate.parse(value).toString(), null, false, by));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException((policyDate ? "The policy date" : entryDate ? "The entry date" : "The period numbering from-date")
                        + " must be yyyy-MM-dd, or blank");
            }
            return;
        }
        if (graceDays) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank");
            }
            values.set(new SettingValues(k, String.valueOf(Integer.parseInt(value)), null, false, by));
            return;
        }
        if (terms) {
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more");
            }
            values.set(new SettingValues(k, String.valueOf(Integer.parseInt(value)), from, true, by));
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
                    : proofThreshold ? "The proof threshold must be an amount with up to two decimals, or blank"
                    : noteThreshold ? "The note threshold must be an amount with up to two decimals, or blank"
                    : "The clerk limit must be an amount with up to two decimals, or blank");
        }
        values.set(new SettingValues(k, value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(),
                from, true, by));
    }
}
