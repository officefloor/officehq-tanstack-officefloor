package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the key is clerk-limit, its value blank (no limit) or an amount 0.00 or more with up to two
 *  decimals; or floor or late-pct, its value such an amount (not blank), all kept with two decimals; or terms, its value a
 *  whole number of days 0 or more (not blank), kept without leading zeros, and so is terms-A, terms-B or terms-C (a
 *  grade's own terms); or grace-A, grace-B or grace-C (a grade's grace), its value blank (none) or a whole number of days 0 or
 *  more, kept without leading zeros; or grace-days, escalate-after or review-sla, its value blank (off) or a whole number of days 0 or more (undated); or pot, or alert-threshold, its value blank (none; off) or an amount with up to two decimals, kept with two (undated); or fiscal-start-month, its value a whole month 1 to 12 (not blank), kept without leading zeros (undated); or policy-date, its value a date yyyy-MM-dd or blank (the sheet's today); or
 *  period-numbering-from or entry-date, its value a date yyyy-MM-dd or blank (off); or cap, proof-threshold, proof-threshold-A, -B or -C (a grade's own) or note-threshold or review-threshold, its value blank (none) or
 *  an amount like the clerk limit; or perm-open, perm-settle, perm-void, perm-reassign, perm-close or perm-reopen, its value
 *  blank (the standing rule) or roles (clerk, steward) separated by commas, kept without spaces or repeats; or proof-kinds, its value blank (none) or kinds (receipt, photo) separated by commas, kept without spaces or repeats (undated); or rounding, its value blank (none) or half-up or half-even; or parent, its
 *  value blank (none) or the id of a sheet that exists (has been loaded), is not this sheet and does not have this sheet
 *  as an ancestor (undated); or default-grade, its value blank (A) or a grade A, B or C (undated). Every key but policy-date, period-numbering-from, entry-date, grace-days, escalate-after, review-sla, pot, alert-threshold, fiscal-start-month, proof-kinds and the permissions is dated: its from-date is blank (the
 *  sheet's today) or a date yyyy-MM-dd. */
public class ValidateSetting {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Set<String> ROLES = Set.of(RoleRepository.CLERK, RoleRepository.STEWARD);

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("key") String key,
            @RequestBody SettingRequest request, SettingRepository settings, SheetRepository sheets,
            Out<SettingValues> values) throws InvalidSettingException {
        String k = key == null ? "" : key.trim();
        boolean floor = SettingRepository.FLOOR.equals(k);
        boolean terms = SettingRepository.TERMS.equals(k) || (k.startsWith(SettingRepository.TERMS_PREFIX)
                && GRADES.contains(k.substring(SettingRepository.TERMS_PREFIX.length())));
        boolean grace = k.startsWith(SettingRepository.GRACE_PREFIX)
                && GRADES.contains(k.substring(SettingRepository.GRACE_PREFIX.length()));
        boolean latePct = SettingRepository.LATE_PCT.equals(k);
        boolean policyDate = SettingRepository.POLICY_DATE.equals(k);
        boolean numberingFrom = SettingRepository.PERIOD_NUMBERING_FROM.equals(k);
        boolean graceDays = SettingRepository.GRACE_DAYS.equals(k);
        boolean escalateAfter = SettingRepository.ESCALATE_AFTER.equals(k);
        boolean reviewSla = SettingRepository.REVIEW_SLA.equals(k);
        boolean entryDate = SettingRepository.ENTRY_DATE.equals(k);
        boolean pot = SettingRepository.POT.equals(k);
        boolean alertThreshold = SettingRepository.ALERT_THRESHOLD.equals(k);
        boolean fiscalStart = SettingRepository.FISCAL_START_MONTH.equals(k);
        boolean defaultGrade = SettingRepository.DEFAULT_GRADE.equals(k);
        boolean cap = SettingRepository.CAP.equals(k);
        boolean proofThreshold = SettingRepository.PROOF_THRESHOLD.equals(k);
        boolean gradeProofThreshold = k.startsWith(SettingRepository.PROOF_THRESHOLD_PREFIX)
                && GRADES.contains(k.substring(SettingRepository.PROOF_THRESHOLD_PREFIX.length()));
        proofThreshold = proofThreshold || gradeProofThreshold;
        boolean proofKinds = SettingRepository.PROOF_KINDS.equals(k);
        boolean noteThreshold = SettingRepository.NOTE_THRESHOLD.equals(k);
        boolean reviewThreshold = SettingRepository.REVIEW_THRESHOLD.equals(k);
        boolean parent = SettingRepository.PARENT.equals(k);
        boolean rounding = SettingRepository.ROUNDING.equals(k);
        boolean perm = k.startsWith(SettingRepository.PERM_PREFIX)
                && SettingRepository.PERMISSIONS.containsKey(k.substring(SettingRepository.PERM_PREFIX.length()));
        if (!proofKinds && !parent && !rounding && !perm && !SettingRepository.CLERK_LIMIT.equals(k) && !cap && !proofThreshold && !noteThreshold && !reviewThreshold && !floor && !terms && !grace && !latePct && !policyDate && !numberingFrom && !graceDays && !escalateAfter && !reviewSla && !entryDate && !pot && !alertThreshold && !fiscalStart && !defaultGrade) {
            throw new InvalidSettingException("Unknown setting " + k);
        }
        String value = request == null || request.value() == null ? "" : request.value().trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String fromText = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!proofKinds && !policyDate && !numberingFrom && !entryDate && !graceDays && !escalateAfter && !reviewSla && !pot && !alertThreshold && !fiscalStart && !defaultGrade && !fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be yyyy-MM-dd, or blank");
            }
        }
        if (parent) {
            String sheet = sheetId.trim();
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            if (value.equals(sheet)) {
                throw new InvalidSettingException("A sheet cannot be its own parent");
            }
            if (!sheets.exists(value)) {
                throw new InvalidSettingException("The parent must be a sheet that exists");
            }
            if (settings.hasAncestor(value, sheet)) {
                throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor");
            }
            values.set(new SettingValues(k, value, null, false, by));
            return;
        }
        if (proofKinds) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            List<String> kinds = new ArrayList<>();
            for (String kind : value.split(",", -1)) {
                String t = kind.trim();
                if (!SettingRepository.KINDS.contains(t)) {
                    throw new InvalidSettingException("The proof kinds must be kinds (receipt, photo) separated by commas, or blank");
                }
                if (!kinds.contains(t)) {
                    kinds.add(t);
                }
            }
            values.set(new SettingValues(k, String.join(",", kinds), null, false, by));
            return;
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
        if (graceDays || escalateAfter || reviewSla) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, null, false, by));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException((escalateAfter ? "The escalation gap" : reviewSla ? "The review SLA" : "The grace days") + " must be a whole number of days, 0 or more, or blank");
            }
            values.set(new SettingValues(k, String.valueOf(Integer.parseInt(value)), null, false, by));
            return;
        }
        if (fiscalStart) {
            int month = DAYS.matcher(value).matches() ? Integer.parseInt(value) : 0;
            if (month < 1 || month > 12) {
                throw new InvalidSettingException("The fiscal year start month must be a whole month, 1 to 12");
            }
            values.set(new SettingValues(k, String.valueOf(month), null, false, by));
            return;
        }
        if (defaultGrade) {
            if (!value.isEmpty() && !GRADES.contains(value)) {
                throw new InvalidSettingException("The default grade must be A, B or C, or blank");
            }
            values.set(new SettingValues(k, value.isEmpty() ? null : value, null, false, by));
            return;
        }
        if (pot || alertThreshold) {
            if (!value.isEmpty() && !AMOUNT.matcher(value).matches()) {
                throw new InvalidSettingException((pot ? "The pot" : "The alert threshold")
                        + " must be an amount with up to two decimals, or blank");
            }
            values.set(new SettingValues(k, value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(),
                    null, false, by));
            return;
        }
        if (rounding) {
            if (!value.isEmpty() && !SettingRepository.HALF_UP.equals(value) && !SettingRepository.HALF_EVEN.equals(value)) {
                throw new InvalidSettingException("The rounding must be half-up or half-even, or blank");
            }
            values.set(new SettingValues(k, value.isEmpty() ? null : value, from, true, by));
            return;
        }
        if (grace) {
            if (value.isEmpty()) {
                values.set(new SettingValues(k, null, from, true, by));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace must be a whole number of days, 0 or more, or blank");
            }
            values.set(new SettingValues(k, String.valueOf(Integer.parseInt(value)), from, true, by));
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
                    : reviewThreshold ? "The review threshold must be an amount with up to two decimals, or blank"
                    : "The clerk limit must be an amount with up to two decimals, or blank");
        }
        values.set(new SettingValues(k, value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(),
                from, true, by));
    }
}
