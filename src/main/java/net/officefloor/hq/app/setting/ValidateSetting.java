package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

public class ValidateSetting {

    public static final String CLERK_LIMIT = "clerk-limit";

    public static final String FLOOR = "floor";

    public static final String CAP = "cap";

    /** Above it (in force on a claim's date) a claim needs a proof to be settled or approved; blank: none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** A grade's proof threshold: proof-threshold-<grade>; dated; blank: none (the sheet's applies). */
    public static final String GRADE_PROOF_THRESHOLD = "proof-threshold-";

    /** Above it (in force on a claim's date) a claim cannot be opened with an empty note; blank: none. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** Above it (in force on a claim's date) a claim goes to REVIEW when opened instead of OPEN; dated; blank: none. */
    public static final String REVIEW_THRESHOLD = "review-threshold";

    /** How a claim's live figures round (in force on its date): half-up or half-even; dated; blank: none (the policy date decides). */
    public static final String ROUNDING = "rounding";

    public static final String TERMS = "terms";

    /** A grade's own terms: terms-<grade>. */
    public static final String GRADE_TERMS = "terms-";

    /** A grade's own clerk limit, followed by the grade: dated like the floor, blank for none (it uses the sheet's). */
    public static final String GRADE_CLERK_LIMIT = "clerk-limit-";

    /** A grade's grace days: grace-<grade>; dated; blank: none. */
    public static final String GRADE_GRACE = "grace-";

    static final List<String> GRADES = List.of("A", "B", "C");

    public static final String LATE_PCT = "late-pct";

    public static final String POLICY_DATE = "policy-date";

    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** Not dated: while set, the default date of a new claim in place of the sheet's today; blank: off. */
    public static final String ENTRY_DATE = "entry-date";

    /** Not dated: when the sheet's today is changed, an open period whose to-date plus them is before it is closed; blank: off. */
    public static final String GRACE_DAYS = "grace-days";

    /** Not dated: reminding a claim whose previous reminder was at least this many days before the sheet's today escalates it; blank: off. */
    public static final String ESCALATE_AFTER = "escalate-after";

    /** Not dated: a REVIEW claim whose review day plus this many days is before the sheet's today is review-late; blank: none. */
    public static final String REVIEW_SLA = "review-sla";

    /** Not dated: a whole number; while it and the score-high are set, a person whose score is at or above it (and below the
     *  score-high) shows the band MID, one below it LOW; blank: none. */
    public static final String SCORE_MID = "score-mid";

    /** Not dated: a whole number; while it and the score-mid are set, a person whose score is at or above it shows the band HIGH; blank: none. */
    public static final String SCORE_HIGH = "score-high";

    /** Not dated: while on, changing the sheet's today audits one SCORE line per person with their score on the day left; blank: off. */
    public static final String SCORE_LOG = "score-log";

    /** A score threshold: a whole number, possibly negative. */
    private static final Pattern SCORE = Pattern.compile("-?\\d{1,6}");

    /** Not dated: each member of a balanced pool shows their cut of it; blank: none. */
    public static final String POT = "pot";

    /** Not dated: a person whose due-total crosses above it is audited as an ALERT; blank: off. */
    public static final String ALERT_THRESHOLD = "alert-threshold";

    /** Not dated: the parent sheet, whose rates (then its parent's, up to three levels up) a grade with none in force here takes; blank: none. */
    public static final String PARENT = "parent";

    /** Not dated: the month (1 to 12) a fiscal year starts in; 1 unless set. */
    public static final String FISCAL_START_MONTH = "fiscal-start-month";

    /** Not dated: a comma separated list of proof kinds (receipt, photo); while set, a claim above the proof threshold needs
     *  at least one proof of each listed kind to be settled or approved; blank: none. */
    public static final String PROOF_KINDS = "proof-kinds";

    /** Not dated: the grade the quick-add bar gives a new person, and an import line with a blank grade takes; blank: A. */
    public static final String DEFAULT_GRADE = "default-grade";

    /** An action's permission: perm-<action>, the roles that may take it. */
    public static final String PERM = "perm-";

    public static final List<String> ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** A number of days: 0 or more, whole. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    /** A percent: 0 or more, with at most two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    /** An amount: 0 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingRequest request, Out<ValidSetting> body, Settings settings, Sheets sheets)
            throws InvalidSettingException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String key = request.key() == null ? "" : request.key().trim();
        if (!CLERK_LIMIT.equals(key) && !FLOOR.equals(key) && !CAP.equals(key) && !PROOF_THRESHOLD.equals(key) && !NOTE_THRESHOLD.equals(key) && !REVIEW_THRESHOLD.equals(key) && !ROUNDING.equals(key) && !TERMS.equals(key) && !LATE_PCT.equals(key)
                && !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !ENTRY_DATE.equals(key) && !GRACE_DAYS.equals(key) && !ESCALATE_AFTER.equals(key) && !REVIEW_SLA.equals(key) && !SCORE_MID.equals(key) && !SCORE_HIGH.equals(key) && !SCORE_LOG.equals(key) && !POT.equals(key) && !ALERT_THRESHOLD.equals(key) && !PARENT.equals(key) && !FISCAL_START_MONTH.equals(key) && !PROOF_KINDS.equals(key) && !DEFAULT_GRADE.equals(key)
                && !(key.startsWith(PERM) && ACTIONS.contains(key.substring(PERM.length())))
                && !(key.startsWith(GRADE_CLERK_LIMIT) && GRADES.contains(key.substring(GRADE_CLERK_LIMIT.length())))
                && !(key.startsWith(GRADE_TERMS) && GRADES.contains(key.substring(GRADE_TERMS.length())))
                && !(key.startsWith(GRADE_GRACE) && GRADES.contains(key.substring(GRADE_GRACE.length())))
                && !(key.startsWith(GRADE_PROOF_THRESHOLD) && GRADES.contains(key.substring(GRADE_PROOF_THRESHOLD.length())))) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        String raw = request.value() == null ? "" : request.value().trim();
        if (key.startsWith(PERM)) {
            // Not dated: a comma separated list of roles; blank for the standing rule.
            List<String> roles = new java.util.ArrayList<>();
            for (String part : raw.isEmpty() ? new String[0] : raw.split(",")) {
                String role = part.trim();
                if (!Settings.CLERK.equals(role) && !Settings.STEWARD.equals(role)) {
                    throw new InvalidSettingException("A permission must be a comma separated list of the roles clerk and steward, or blank for the standing rule");
                }
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, String.join(",", roles)));
            return;
        }
        if (PROOF_KINDS.equals(key)) {
            // Not dated: a comma separated list of proof kinds; blank for none.
            List<String> kinds = new java.util.ArrayList<>();
            for (String part : raw.isEmpty() ? new String[0] : raw.split(",")) {
                String kind = part.trim();
                if (!net.officefloor.hq.app.claim.ProofKind.KINDS.contains(kind)) {
                    throw new InvalidSettingException("The proof kinds must be a comma separated list of the kinds receipt and photo, or blank for none");
                }
                if (!kinds.contains(kind)) {
                    kinds.add(kind);
                }
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, null, String.join(",", kinds)));
            return;
        }
        if (DEFAULT_GRADE.equals(key)) {
            if (!raw.isEmpty() && !GRADES.contains(raw)) {
                throw new InvalidSettingException("The default grade must be one of the grades A, B and C, or blank for A");
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, null, raw));
            return;
        }
        if (FISCAL_START_MONTH.equals(key)) {
            int month = raw.matches("\\d{1,2}") ? Integer.parseInt(raw) : 0;
            if (month < 1 || month > 12) {
                throw new InvalidSettingException("The fiscal year start month must be a month from 1 to 12");
            }
            body.set(new ValidSetting(key, new BigDecimal(month), request.user(), null, null));
            return;
        }
        if (ESCALATE_AFTER.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The escalation gap must be a whole number of days, 0 or more, or blank for off");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, null));
            return;
        }
        if (REVIEW_SLA.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: none
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The review SLA must be a whole number of days, 0 or more, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, null));
            return;
        }
        if (SCORE_LOG.equals(key)) {
            if (!raw.isEmpty() && !"on".equals(raw)) {
                throw new InvalidSettingException("The score log must be on, or blank for off");
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, null, raw.isEmpty() ? null : raw));
            return;
        }
        if (SCORE_MID.equals(key) || SCORE_HIGH.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: none
                return;
            }
            if (!SCORE.matcher(raw).matches()) {
                throw new InvalidSettingException("A score threshold must be a whole number, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, null));
            return;
        }
        if (GRACE_DAYS.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank for off");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, null));
            return;
        }
        if (PARENT.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: none
                return;
            }
            if (raw.equals(sheetId)) {
                throw new InvalidSettingException("A sheet cannot be its own parent");
            }
            if (!sheets.exists(raw)) {
                throw new InvalidSettingException("The parent must be a sheet that has been loaded");
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String at = raw; at != null && seen.add(at); at = sheets.parent(at)) {
                if (at.equals(sheetId)) {
                    throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor");
                }
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, null, raw));
            return;
        }
        if (ALERT_THRESHOLD.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The alert threshold must be an amount of 0.00 or more with at most two decimals, or blank for off");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, null));
            return;
        }
        if (POT.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The pot must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, null));
            return;
        }
        // A dated setting's from-date: blank for the sheet's today.
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!rawFrom.isEmpty() && !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !ENTRY_DATE.equals(key)) {
            try {
                from = LocalDate.parse(rawFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be a date (YYYY-MM-DD), or blank for the sheet's today");
            }
        }
        if (POLICY_DATE.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: the sheet's today
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The policy date must be a date (YYYY-MM-DD), or blank for the sheet's today");
            }
            return;
        }
        if (PERIOD_NUMBERING_FROM.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The period numbering from must be a date (YYYY-MM-DD), or blank for off");
            }
            return;
        }
        if (ENTRY_DATE.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The entry date must be a date (YYYY-MM-DD), or blank for off");
            }
            return;
        }
        if (ROUNDING.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from, null, "")); // blank: none
                return;
            }
            if (net.officefloor.hq.app.claim.Rounding.code(raw) == null) {
                throw new InvalidSettingException("The rounding mode must be half-up or half-even, or blank for none");
            }
            body.set(new ValidSetting(key, net.officefloor.hq.app.claim.Rounding.code(raw), request.user(), null, from, null, raw));
            return;
        }
        if (TERMS.equals(key) || key.startsWith(GRADE_TERMS)) {
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, from));
            return;
        }
        if (!GRACE_DAYS.equals(key) && key.startsWith(GRADE_GRACE)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("A grade's grace days must be a whole number of days, 0 or more, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, from));
            return;
        }
        if (LATE_PCT.equals(key)) {
            if (!PERCENT.matcher(raw).matches()) {
                throw new InvalidSettingException("The late percent must be a percent of 0.00 or more with at most two decimals");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (CAP.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: no cap
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The cap must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (PROOF_THRESHOLD.equals(key) || key.startsWith(GRADE_PROOF_THRESHOLD)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The proof threshold must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (REVIEW_THRESHOLD.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The review threshold must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (NOTE_THRESHOLD.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The note threshold must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (FLOOR.equals(key)) {
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The floor must be an amount of 0.00 or more with at most two decimals");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (raw.isEmpty()) {
            body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: no limit
            return;
        }
        if (!AMOUNT.matcher(raw).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more with at most two decimals");
        }
        body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
    }
}
