package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.role.Role;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a clerk limit (when given)
 *  that is blank (no limit) or an amount 0.00 or more with up to two decimals, and a floor (when given) that is an
 *  amount 0.00 or more with up to two decimals, and terms (when given) that
 *  are a whole number of days, and a late percent (when given) that is a percent 0.00 or more with up to two decimals, and a policy date (when given) that is
 *  blank (the sheet's today) or a date yyyy-mm-dd, and a period numbering from (when given) that is blank (off) or a date yyyy-mm-dd, and grades' own terms (when given)
 *  only for grades A, B or C, and a cap and a proof threshold (when given) each blank (none) or an amount 0.00 or more with up to two decimals, each blank (none: the sheet's terms) or a whole number of days, and from-dates (when given) only for the dated settings, each blank (the sheet's
 *  today) or a date yyyy-mm-dd, and permission entries (when given) only for open, settle, void, reassign, close or
 *  reopen, each blank (the standing rule) or a comma separated list of clerk and steward, and grace days (when given) blank (off) or a whole number of days, and a note threshold (when given) blank (none) or an amount 0.00 or more with up to two decimals, and an entry date (when given) blank (off) or a date yyyy-mm-dd, and grades' grace (when given) only for grades A, B or C, each blank (none) or a whole number of days, and a pot (when given) blank (none) or an amount 0.00 or more with up to two decimals, and an escalation gap (when given) blank (off) or a whole number of days, and proof kinds (when given) blank (none) or a comma separated list of receipt and photo, and a default grade (when given) blank (A) or A, B or C. A rounding mode (when given) is blank (none) or half-up or half-even. A fiscal year start month (when given) is a month 1 to 12. The parent sheet (when given) is
 *  checked against the store by ValidateParent. */
public class ValidateSettings {
    /** The grades that may have terms of their own. */
    static final List<String> GRADES = List.of("A", "B", "C");

    private static final String PERCENT = "\\d{1,3}(\\.\\d{1,2})?";
    private static final String AMOUNT = "\\d{1,12}(\\.\\d{1,2})?";

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSettingException("Sign in before changing settings.");
        }
        String limit = request.clerkLimit() == null ? null : request.clerkLimit().trim();
        if (limit != null && !limit.isEmpty() && !limit.matches(AMOUNT)) {
            throw new InvalidSettingException("The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.");
        }
        String floor = request.floor() == null ? null : request.floor().trim();
        if (floor != null && !floor.matches(AMOUNT)) {
            throw new InvalidSettingException("The floor must be an amount, 0.00 or more, with up to two decimals.");
        }
        String terms = request.terms() == null ? null : request.terms().trim();
        if (terms != null && !terms.matches("\\d{1,4}")) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more.");
        }
        String latePct = request.latePct() == null ? null : request.latePct().trim();
        if (latePct != null && !latePct.matches(PERCENT)) {
            throw new InvalidSettingException("The late percent must be a percent, 0.00 or more, with up to two decimals.");
        }
        String policy = request.policyDate() == null ? null : request.policyDate().trim();
        LocalDate policyDate = null;
        if (policy != null && !policy.isEmpty()) {
            try {
                policyDate = LocalDate.parse(policy);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The policy date must be a date, yyyy-mm-dd, or blank for the sheet's today.");
            }
        }
        String numbering = request.periodNumberingFrom() == null ? null : request.periodNumberingFrom().trim();
        LocalDate numberingFrom = null;
        if (numbering != null && !numbering.isEmpty()) {
            try {
                numberingFrom = LocalDate.parse(numbering);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The period numbering from must be a date, yyyy-mm-dd, or blank for off.");
            }
        }
        String cap = request.cap() == null ? null : request.cap().trim();
        if (cap != null && !cap.isEmpty() && !cap.matches(AMOUNT)) {
            throw new InvalidSettingException("The cap must be an amount, 0.00 or more, with up to two decimals, or blank for none.");
        }
        String threshold = request.proofThreshold() == null ? null : request.proofThreshold().trim();
        if (threshold != null && !threshold.isEmpty() && !threshold.matches(AMOUNT)) {
            throw new InvalidSettingException("The proof threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.");
        }
        String noteThreshold = request.noteThreshold() == null ? null : request.noteThreshold().trim();
        if (noteThreshold != null && !noteThreshold.isEmpty() && !noteThreshold.matches(AMOUNT)) {
            throw new InvalidSettingException("The note threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.");
        }
        String reviewThreshold = request.reviewThreshold() == null ? null : request.reviewThreshold().trim();
        if (reviewThreshold != null && !reviewThreshold.isEmpty() && !reviewThreshold.matches(AMOUNT)) {
            throw new InvalidSettingException("The review threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.");
        }
        String grace = request.graceDays() == null ? null : request.graceDays().trim();
        if (grace != null && !grace.isEmpty() && !grace.matches("\\d{1,4}")) {
            throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank for off.");
        }
        String escalate = request.escalateAfter() == null ? null : request.escalateAfter().trim();
        if (escalate != null && !escalate.isEmpty() && !escalate.matches("\\d{1,4}")) {
            throw new InvalidSettingException("The escalation gap must be a whole number of days, 0 or more, or blank for off.");
        }
        String reviewSla = request.reviewSla() == null ? null : request.reviewSla().trim();
        if (reviewSla != null && !reviewSla.isEmpty() && !reviewSla.matches("\\d{1,4}")) {
            throw new InvalidSettingException("The review SLA must be a whole number of days, 0 or more, or blank for none.");
        }
        String scoreMid = request.scoreMid() == null ? null : request.scoreMid().trim();
        if (scoreMid != null && !scoreMid.isEmpty() && !scoreMid.matches("-?\\d{1,6}")) {
            throw new InvalidSettingException("The mid score threshold must be a whole number, or blank for none.");
        }
        String scoreHigh = request.scoreHigh() == null ? null : request.scoreHigh().trim();
        if (scoreHigh != null && !scoreHigh.isEmpty() && !scoreHigh.matches("-?\\d{1,6}")) {
            throw new InvalidSettingException("The high score threshold must be a whole number, or blank for none.");
        }
        String proofKinds = request.proofKinds() == null ? null : proofKinds(request.proofKinds());
        String defaultGrade = request.defaultGrade() == null ? null : request.defaultGrade().trim();
        if (defaultGrade != null && !defaultGrade.isEmpty() && !GRADES.contains(defaultGrade)) {
            throw new InvalidSettingException("The default grade must be A, B or C, or blank for A.");
        }
        String entered = request.entryDate() == null ? null : request.entryDate().trim();
        LocalDate entryDate = null;
        if (entered != null && !entered.isEmpty()) {
            try {
                entryDate = LocalDate.parse(entered);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The entry date must be a date, yyyy-mm-dd, or blank for off.");
            }
        }
        String pot = request.pot() == null ? null : request.pot().trim();
        if (pot != null && !pot.isEmpty() && !pot.matches(AMOUNT)) {
            throw new InvalidSettingException("The pot must be an amount, 0.00 or more, with up to two decimals, or blank for none.");
        }
        String alert = request.alertThreshold() == null ? null : request.alertThreshold().trim();
        if (alert != null && !alert.isEmpty() && !alert.matches(AMOUNT)) {
            throw new InvalidSettingException("The alert threshold must be an amount, 0.00 or more, with up to two decimals, or blank for off.");
        }
        String rounding = request.rounding() == null ? null : request.rounding().trim();
        if (rounding != null && !rounding.isEmpty() && !rounding.equals(SettingHistory.HALF_UP)
                && !rounding.equals(SettingHistory.HALF_EVEN)) {
            throw new InvalidSettingException("The rounding mode must be half-up or half-even, or blank for none.");
        }
        String parent = request.parent() == null ? null : request.parent().trim();
        String fiscal = request.fiscalStartMonth() == null ? null : request.fiscalStartMonth().trim();
        if (fiscal != null && !fiscal.matches("[1-9]|1[0-2]")) {
            throw new InvalidSettingException("The fiscal year start month must be a month, 1 to 12.");
        }
        Map<String, Integer> gradeTerms = new LinkedHashMap<>();
        if (request.gradeTerms() != null) {
            for (String grade : request.gradeTerms().keySet()) {
                if (!GRADES.contains(grade)) {
                    throw new InvalidSettingException("Terms may be set only for grade A, B or C.");
                }
            }
            for (String grade : GRADES) {
                if (!request.gradeTerms().containsKey(grade)) {
                    continue;
                }
                String days = request.gradeTerms().get(grade) == null ? "" : request.gradeTerms().get(grade).trim();
                if (!days.isEmpty() && !days.matches("\\d{1,4}")) {
                    throw new InvalidSettingException("The terms of grade " + grade
                            + " must be a whole number of days, 0 or more, or blank for the sheet's terms.");
                }
                gradeTerms.put(grade, days.isEmpty() ? null : Integer.valueOf(days));
            }
        }
        Map<String, Integer> gradeGrace = new LinkedHashMap<>();
        if (request.gradeGrace() != null) {
            for (String grade : request.gradeGrace().keySet()) {
                if (!GRADES.contains(grade)) {
                    throw new InvalidSettingException("Grace may be set only for grade A, B or C.");
                }
            }
            for (String grade : GRADES) {
                if (!request.gradeGrace().containsKey(grade)) {
                    continue;
                }
                String days = request.gradeGrace().get(grade) == null ? "" : request.gradeGrace().get(grade).trim();
                if (!days.isEmpty() && !days.matches("\\d{1,4}")) {
                    throw new InvalidSettingException("The grace of grade " + grade
                            + " must be a whole number of days, 0 or more, or blank for none.");
                }
                gradeGrace.put(grade, days.isEmpty() ? null : Integer.valueOf(days));
            }
        }
        Map<String, BigDecimal> gradeProofThreshold = new LinkedHashMap<>();
        if (request.gradeProofThreshold() != null) {
            for (String grade : request.gradeProofThreshold().keySet()) {
                if (!GRADES.contains(grade)) {
                    throw new InvalidSettingException("A proof threshold may be set only for grade A, B or C.");
                }
            }
            for (String grade : GRADES) {
                if (!request.gradeProofThreshold().containsKey(grade)) {
                    continue;
                }
                String amount = request.gradeProofThreshold().get(grade) == null ? ""
                        : request.gradeProofThreshold().get(grade).trim();
                if (!amount.isEmpty() && !amount.matches(AMOUNT)) {
                    throw new InvalidSettingException("The proof threshold of grade " + grade
                            + " must be an amount, 0.00 or more, with up to two decimals, or blank for the sheet's.");
                }
                gradeProofThreshold.put(grade, amount.isEmpty() ? null : new BigDecimal(amount).setScale(2));
            }
        }
        Map<String, BigDecimal> gradeClerkLimit = new LinkedHashMap<>();
        if (request.gradeClerkLimit() != null) {
            for (String grade : request.gradeClerkLimit().keySet()) {
                if (!GRADES.contains(grade)) {
                    throw new InvalidSettingException("A clerk limit may be set only for grade A, B or C.");
                }
            }
            for (String grade : GRADES) {
                if (!request.gradeClerkLimit().containsKey(grade)) {
                    continue;
                }
                String amount = request.gradeClerkLimit().get(grade) == null ? ""
                        : request.gradeClerkLimit().get(grade).trim();
                if (!amount.isEmpty() && !amount.matches(AMOUNT)) {
                    throw new InvalidSettingException("The clerk limit of grade " + grade
                            + " must be an amount, 0.00 or more, with up to two decimals, or blank for the sheet's.");
                }
                gradeClerkLimit.put(grade, amount.isEmpty() ? null : new BigDecimal(amount).setScale(2));
            }
        }
        Map<String, String> perms = new LinkedHashMap<>();
        if (request.perms() != null) {
            for (String action : request.perms().keySet()) {
                if (!Permissions.ACTIONS.contains(action)) {
                    throw new InvalidSettingException("Permissions may be set only for open, settle, void, reassign, close or reopen.");
                }
            }
            for (String action : Permissions.ACTIONS) {
                if (!request.perms().containsKey(action)) {
                    continue;
                }
                String entry = request.perms().get(action) == null ? "" : request.perms().get(action).trim();
                List<String> roles = new ArrayList<>();
                if (!entry.isEmpty()) {
                    for (String role : entry.split(",", -1)) {
                        String r = role.trim();
                        if (!r.equals(Role.CLERK) && !r.equals(Role.STEWARD)) {
                            throw new InvalidSettingException("The roles allowed to " + action
                                    + " must be a comma separated list of clerk and steward, or blank for the standing rule.");
                        }
                        if (!roles.contains(r)) {
                            roles.add(r);
                        }
                    }
                }
                perms.put(action, String.join(",", roles));
            }
        }
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (request.from() != null) {
            for (var e : request.from().entrySet()) {
                if (!SettingHistory.KEYS.contains(e.getKey())) {
                    throw new InvalidSettingException("Only a setting that carries a figure takes a from-date.");
                }
                String date = e.getValue() == null ? "" : e.getValue().trim();
                if (date.isEmpty()) {
                    continue;
                }
                try {
                    from.put(e.getKey(), LocalDate.parse(date));
                } catch (DateTimeParseException ex) {
                    throw new InvalidSettingException("The from-date must be a date, yyyy-mm-dd, or blank for the sheet's today.");
                }
            }
        }
        body.set(new SettingsInput(request.user().trim(), limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : new BigDecimal(floor).setScale(2),
                terms == null ? null : Integer.valueOf(terms),
                latePct == null ? null : new BigDecimal(latePct).setScale(2),
                policy != null, policyDate, numbering != null, numberingFrom, gradeTerms,
                cap != null, cap == null || cap.isEmpty() ? null : new BigDecimal(cap).setScale(2), from,
                threshold != null, threshold == null || threshold.isEmpty() ? null : new BigDecimal(threshold).setScale(2),
                perms, grace != null, grace == null || grace.isEmpty() ? null : Integer.valueOf(grace),
                noteThreshold != null,
                noteThreshold == null || noteThreshold.isEmpty() ? null : new BigDecimal(noteThreshold).setScale(2),
                entered != null, entryDate, gradeGrace, gradeProofThreshold,
                pot != null, pot == null || pot.isEmpty() ? null : new BigDecimal(pot).setScale(2),
                parent != null, parent == null || parent.isEmpty() ? null : parent,
                fiscal == null ? null : Integer.valueOf(fiscal),
                alert != null, alert == null || alert.isEmpty() ? null : new BigDecimal(alert).setScale(2),
                rounding != null, rounding == null || rounding.isEmpty() ? null : rounding,
                reviewThreshold != null,
                reviewThreshold == null || reviewThreshold.isEmpty() ? null : new BigDecimal(reviewThreshold).setScale(2),
                escalate != null, escalate == null || escalate.isEmpty() ? null : Integer.valueOf(escalate),
                reviewSla != null, reviewSla == null || reviewSla.isEmpty() ? null : Integer.valueOf(reviewSla),
                proofKinds != null, proofKinds == null || proofKinds.isEmpty() ? null : proofKinds,
                defaultGrade != null, defaultGrade == null || defaultGrade.isEmpty() ? null : defaultGrade,
                gradeClerkLimit,
                scoreMid != null, scoreMid == null || scoreMid.isEmpty() ? null : Integer.valueOf(scoreMid),
                scoreHigh != null, scoreHigh == null || scoreHigh.isEmpty() ? null : Integer.valueOf(scoreHigh)));
    }

    /** The proof kinds as stored: each listed kind trimmed, in the order given, without repeats, joined by commas ('':
     *  none); refused when one is not a proof kind (receipt or photo). */
    private static String proofKinds(String list) throws InvalidSettingException {
        var kinds = new java.util.LinkedHashSet<String>();
        for (String kind : list.split(",", -1)) {
            String k = kind.trim();
            if (k.isEmpty() && list.isBlank()) {
                continue;
            }
            if (!net.officefloor.hq.app.claim.Claim.PROOF_KINDS.contains(k)) {
                throw new InvalidSettingException("The proof kinds must be a comma separated list of "
                        + String.join(" and ", net.officefloor.hq.app.claim.Claim.PROOF_KINDS) + ", or blank for none.");
            }
            kinds.add(k);
        }
        return String.join(",", kinds);
    }
}
