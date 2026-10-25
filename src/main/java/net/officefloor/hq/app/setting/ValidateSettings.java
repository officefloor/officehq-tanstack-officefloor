package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ProofKind;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings form from the body; refuses a clerk limit or floor that is negative or has more than two
 *  decimals, and terms that are not a whole number of days, and a late percent that is negative or has more than two decimals, and a policy date or period numbering from that is not a calendar date
 *  (YYYY-MM-DD). A blank clerk limit means no limit; a blank floor is
 *  1.00; blank terms are 14; a blank late percent is 0.00; a blank policy date is the sheet's today; a blank period numbering from is off; a blank entry date is off; a blank cap is none (else an amount like the floor); a blank proof threshold is none (else an amount like the floor); a blank note threshold is none (else an amount like the floor). A grade's terms must be a whole number of days, or blank (the grade uses the sheet's terms). A permission matrix entry must be a comma separated list of
 *  roles (clerk, steward), or blank (the standing rule). The grace days must be a whole number of days, or blank (off). A grade's grace days must be a whole number of days, or blank (none). A grade's proof threshold is an amount like the floor, or blank (none: the sheet's applies). The pot is an amount like the floor, or blank (none); not dated. The fiscal year start month is a month 1 to 12, or blank (1); not dated. The alert threshold is an amount like the floor, or blank (off); not dated. The rounding mode is half-up or half-even, or blank (none); dated. A blank review threshold is none (else an amount like the floor); dated. The escalation gap must be a whole number of days, or blank (off); not dated. The proof kinds are a comma separated list of receipt and photo, or blank (none); not dated. The default grade is A, B or C, or blank (A); not dated. A grade's clerk limit is an amount like the floor, or blank (none: the sheet's applies); dated. A key left out is not changed.
 *  A from-date must be a calendar date (YYYY-MM-DD), or blank (the sheet's today). */
public class ValidateSettings {

    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Pattern WHOLE = Pattern.compile("-?\\d{1,9}");

    private static final Pattern MONTH = Pattern.compile("\\d{1,2}");

    private static final Set<String> GRADES = Set.of("A", "B", "C");

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
            date(changes, Sheet.ENTRY_DATE, request.entryDate(), "The entry date must be a date (YYYY-MM-DD), or blank.");
            days(changes, Sheet.TERMS + "-A", request.termsA(), null, request.termsAFrom(),
                    "The terms of grade A must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.TERMS + "-B", request.termsB(), null, request.termsBFrom(),
                    "The terms of grade B must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.TERMS + "-C", request.termsC(), null, request.termsCFrom(),
                    "The terms of grade C must be a whole number of days, 0 or more, or blank.");
            amount(changes, Sheet.CAP, request.cap(), null, request.capFrom(),
                    "The cap must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.PROOF_THRESHOLD, request.proofThreshold(), null, request.proofThresholdFrom(),
                    "The proof threshold must be an amount, 0.00 or more, with up to two decimals, or blank.");
            roles(changes, "open", request.permOpen());
            roles(changes, "settle", request.permSettle());
            roles(changes, "void", request.permVoid());
            roles(changes, "reassign", request.permReassign());
            roles(changes, "close", request.permClose());
            roles(changes, "reopen", request.permReopen());
            graceDays(changes, request.graceDays());
            amount(changes, Sheet.NOTE_THRESHOLD, request.noteThreshold(), null, request.noteThresholdFrom(),
                    "The note threshold must be an amount, 0.00 or more, with up to two decimals, or blank.");
            days(changes, Sheet.graceKey("A"), request.graceA(), null, request.graceAFrom(),
                    "The grace days of grade A must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.graceKey("B"), request.graceB(), null, request.graceBFrom(),
                    "The grace days of grade B must be a whole number of days, 0 or more, or blank.");
            days(changes, Sheet.graceKey("C"), request.graceC(), null, request.graceCFrom(),
                    "The grace days of grade C must be a whole number of days, 0 or more, or blank.");
            amount(changes, Sheet.proofThresholdKey("A"), request.proofThresholdA(), null, request.proofThresholdAFrom(),
                    "The proof threshold of grade A must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.proofThresholdKey("B"), request.proofThresholdB(), null, request.proofThresholdBFrom(),
                    "The proof threshold of grade B must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.proofThresholdKey("C"), request.proofThresholdC(), null, request.proofThresholdCFrom(),
                    "The proof threshold of grade C must be an amount, 0.00 or more, with up to two decimals, or blank.");
            pot(changes, request.pot());
            parent(changes, request.parent());
            fiscalStartMonth(changes, request.fiscalStartMonth());
            undatedAmount(changes, Sheet.ALERT_THRESHOLD, request.alertThreshold(),
                    "The alert threshold must be an amount, 0.00 or more, with up to two decimals, or blank.");
            rounding(changes, request.rounding(), request.roundingFrom());
            amount(changes, Sheet.REVIEW_THRESHOLD, request.reviewThreshold(), null, request.reviewThresholdFrom(),
                    "The review threshold must be an amount, 0.00 or more, with up to two decimals, or blank.");
            undatedDays(changes, Sheet.ESCALATE_AFTER, request.escalateAfter(),
                    "The escalation gap must be a whole number of days, 0 or more, or blank.");
            undatedDays(changes, Sheet.REVIEW_SLA, request.reviewSla(),
                    "The review SLA must be a whole number of days, 0 or more, or blank.");
            undatedDays(changes, Sheet.PENDING_SLA, request.pendingSla(),
                    "The pending SLA must be a whole number of days, 0 or more, or blank.");
            proofKinds(changes, request.proofKinds());
            defaultGrade(changes, request.defaultGrade());
            amount(changes, Sheet.clerkLimitKey("A"), request.clerkLimitA(), null, request.clerkLimitAFrom(),
                    "The clerk limit of grade A must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.clerkLimitKey("B"), request.clerkLimitB(), null, request.clerkLimitBFrom(),
                    "The clerk limit of grade B must be an amount, 0.00 or more, with up to two decimals, or blank.");
            amount(changes, Sheet.clerkLimitKey("C"), request.clerkLimitC(), null, request.clerkLimitCFrom(),
                    "The clerk limit of grade C must be an amount, 0.00 or more, with up to two decimals, or blank.");
            wholeNumber(changes, Sheet.SCORE_MID, request.scoreMid(), "The mid score threshold must be a whole number, or blank.");
            wholeNumber(changes, Sheet.SCORE_HIGH, request.scoreHigh(), "The high score threshold must be a whole number, or blank.");
            onOrBlank(changes, Sheet.SCORE_LOG, request.scoreLog(), "The score log must be on, or blank.");
            undatedDays(changes, Sheet.MAX_OPEN, request.maxOpen(),
                    "The maximum of open claims must be a whole number, 0 or more, or blank.");
        }
        body.set(new SettingsInput(changes));
    }

    /** Adds the proof kinds when the form sets them: a comma separated list of kinds (receipt, photo), each once, or
     *  blank (none); not dated. */
    private static void proofKinds(List<SettingsInput.Change> changes, String value) throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        List<String> kinds = new ArrayList<>();
        if (!value.trim().isEmpty()) {
            for (String part : value.split(",", -1)) {
                String kind = part.trim();
                if (!ProofKind.ALL.contains(kind) || kinds.contains(kind)) {
                    throw new InvalidSettingsException("The proof kinds must be a comma separated list of receipt and photo, or blank.");
                }
                kinds.add(kind);
            }
        }
        changes.add(new SettingsInput.Change(Sheet.PROOF_KINDS, kinds.isEmpty() ? null : String.join(",", kinds), false, null));
    }

    /** Adds the default grade when the form sets it: A, B or C, or blank (A); not dated. */
    private static void defaultGrade(List<SettingsInput.Change> changes, String value) throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String grade = value.trim().isEmpty() ? null : value.trim();
        if (grade != null && !GRADES.contains(grade)) {
            throw new InvalidSettingsException("The default grade must be A, B or C, or blank.");
        }
        changes.add(new SettingsInput.Change(Sheet.DEFAULT_GRADE, grade, false, null));
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

    /** Adds an action's permission matrix entry when the form sets it: comma separated roles, each clerk or steward
     *  (blank: null, the standing rule). Undated. */
    private static void roles(List<SettingsInput.Change> changes, String action, String value)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String roles = null;
        if (!value.trim().isEmpty()) {
            List<String> each = new ArrayList<>();
            for (String role : value.split(",", -1)) {
                if (!Role.CLERK.equals(role.trim()) && !Role.STEWARD.equals(role.trim())) {
                    throw new InvalidSettingsException("The roles that may " + action
                            + " must be a comma separated list of clerk and steward, or blank.");
                }
                if (!each.contains(role.trim())) {
                    each.add(role.trim());
                }
            }
            roles = String.join(",", each);
        }
        changes.add(new SettingsInput.Change(Sheet.permKey(action), roles, false, null));
    }

    /** Adds the grace days when the form sets them: a whole number of days, or blank (off). Undated. */
    private static void graceDays(List<SettingsInput.Change> changes, String value) throws InvalidSettingsException {
        undatedDays(changes, Sheet.GRACE_DAYS, value, "The grace days must be a whole number of days, 0 or more, or blank.");
    }

    /** Adds an undated switch when the form sets it: on, or blank (off); not dated. */
    private static void onOrBlank(List<SettingsInput.Change> changes, String key, String value, String message)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String on = null;
        if (!value.trim().isEmpty()) {
            if (!"on".equals(value.trim())) {
                throw new InvalidSettingsException(message);
            }
            on = "on";
        }
        changes.add(new SettingsInput.Change(key, on, false, null));
    }

    /** Adds an undated whole-number setting (a score threshold) when the form sets it: a whole number, possibly
     *  negative, or blank; not dated. */
    private static void wholeNumber(List<SettingsInput.Change> changes, String key, String value, String message)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String number = null;
        if (!value.trim().isEmpty()) {
            if (!WHOLE.matcher(value.trim()).matches()) {
                throw new InvalidSettingsException(message);
            }
            number = String.valueOf(Integer.parseInt(value.trim()));
        }
        changes.add(new SettingsInput.Change(key, number, false, null));
    }

    /** Adds an undated days setting when the form sets it: a whole number of days, or blank (off). */
    private static void undatedDays(List<SettingsInput.Change> changes, String key, String value, String message)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String days = null;
        if (!value.trim().isEmpty()) {
            if (!DAYS.matcher(value.trim()).matches()) {
                throw new InvalidSettingsException(message);
            }
            days = String.valueOf(Integer.parseInt(value.trim()));
        }
        changes.add(new SettingsInput.Change(key, days, false, null));
    }

    /** Adds the pot when the form sets it: an amount with up to two decimals, or blank (none). Undated. */
    private static void pot(List<SettingsInput.Change> changes, String value) throws InvalidSettingsException {
        undatedAmount(changes, Sheet.POT, value, "The pot must be an amount, 0.00 or more, with up to two decimals, or blank.");
    }

    /** Adds an undated amount setting when the form sets it: an amount with up to two decimals, or blank (none). */
    private static void undatedAmount(List<SettingsInput.Change> changes, String key, String value, String message)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String amount = null;
        if (!value.trim().isEmpty()) {
            if (!AMOUNT.matcher(value.trim()).matches()) {
                throw new InvalidSettingsException(message);
            }
            amount = new BigDecimal(value.trim()).setScale(2).toPlainString();
        }
        changes.add(new SettingsInput.Change(key, amount, false, null));
    }

    /** Adds the parent sheet when the form sets it: a sheet's id, or blank (none). Undated. Whether that sheet may
     *  be the parent is checked once the sheet is loaded ({@link ValidateParent}). */
    private static void parent(List<SettingsInput.Change> changes, String value) {
        if (value == null) {
            return;
        }
        changes.add(new SettingsInput.Change(Sheet.PARENT, value.trim().isEmpty() ? null : value.trim(), false, null));
    }

    /** Adds the fiscal year start month when the form sets it: a month 1 to 12, or blank (1). Undated. */
    private static void fiscalStartMonth(List<SettingsInput.Change> changes, String value)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        int month = 1;
        if (!value.trim().isEmpty()) {
            if (!MONTH.matcher(value.trim()).matches() || Integer.parseInt(value.trim()) < 1
                    || Integer.parseInt(value.trim()) > 12) {
                throw new InvalidSettingsException("The fiscal year start month must be a month, 1 to 12, or blank.");
            }
            month = Integer.parseInt(value.trim());
        }
        changes.add(new SettingsInput.Change(Sheet.FISCAL_START_MONTH, String.valueOf(month), false, null));
    }

    /** Adds the rounding mode when the form sets it: half-up or half-even, or blank (none). Dated. */
    private static void rounding(List<SettingsInput.Change> changes, String value, String from)
            throws InvalidSettingsException {
        if (value == null) {
            return;
        }
        String mode = value.trim().isEmpty() ? null : value.trim();
        if (mode != null && !Sheet.HALF_UP.equals(mode) && !Sheet.HALF_EVEN.equals(mode)) {
            throw new InvalidSettingsException("The rounding mode must be half-up or half-even, or blank.");
        }
        changes.add(new SettingsInput.Change(Sheet.ROUNDING, mode, true, from(Sheet.ROUNDING, from)));
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
