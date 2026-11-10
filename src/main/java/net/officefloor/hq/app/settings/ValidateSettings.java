package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the settings form: the clerk limit is 0.00 or more with up to two decimals, blank meaning no
 *  limit; the floor is 0.00 or more with up to two decimals; the terms are a whole number of days, 0 to
 *  3650; the late percent is 0.00 or more with up to two decimals; the policy date is a date
 *  (yyyy-mm-dd), blank meaning the sheet's today; the period
 *  numbering from-date is a date, blank meaning off; a grade's terms are like the sheet's, blank
 *  meaning the grade uses the sheet's terms; the cap is 0.00 or more with up to two decimals, blank meaning none; the proof threshold and the note threshold likewise. A from-date given for a
 *  setting with a figure is a date; blank is the sheet's today. A permission matrix entry (open, settle, void, reassign, close or reopen) is a comma separated
 *  list of roles (clerk, steward), blank keeping the standing rule. The grace days are a whole number of days, 0 to 3650, blank meaning off. The entry date is a date, blank meaning off. A grade's grace days are a whole number of days, 0 to 3650, blank meaning none. A grade's proof threshold is like the sheet's, blank meaning none. A grade's clerk limit is like the sheet's, blank meaning none. The pot is 0.00 or more with up to two decimals, blank meaning none. The parent is a sheet id, blank meaning none (checked when saved). The fiscal year start month is a month, 1 to 12. The alert threshold is 0.00 or more with up to two decimals, blank meaning off. The rounding mode is half-up or half-even, blank meaning none. The escalation gap is a whole number of days, 0 to 3650, blank meaning off. The default grade is a grade code, blank meaning A. Every grade named must be one of the sheet's grades (checked when saved). A setting left out is not changed. */
public class ValidateSettings {
    private static final Pattern MONEY = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?|\\.\\d{1,2}");
    private static final Pattern FISCAL_MONTH = Pattern.compile("\\d{1,2}");
    private static final Pattern SCORE = Pattern.compile("-?\\d{1,9}");
    private static final Pattern TERMS = Pattern.compile("\\d{1,4}");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> settings)
            throws BadRequestException {
        String limit = request == null || request.clerkLimit() == null ? null : request.clerkLimit().trim();
        String floor = request == null || request.floor() == null ? null : request.floor().trim();
        String terms = request == null || request.terms() == null ? null : request.terms().trim();
        if (limit != null && !limit.isEmpty() && !MONEY.matcher(limit).matches()) {
            throw new BadRequestException("clerk limit must be 0.00 or more with up to two decimals");
        }
        if (floor != null && !MONEY.matcher(floor).matches()) {
            throw new BadRequestException("floor must be 0.00 or more with up to two decimals");
        }
        String latePct = request == null || request.latePct() == null ? null : request.latePct().trim();
        if (terms != null && (!TERMS.matcher(terms).matches() || Integer.parseInt(terms) > 3650)) {
            throw new BadRequestException("terms must be a whole number of days from 0 to 3650");
        }
        Map<String, Integer> gradeTerms = new LinkedHashMap<>();
        Map<String, String> grades = request == null || request.gradeTerms() == null ? Map.of() : request.gradeTerms();
        for (String grade : gradeOrder(grades.keySet())) {
            String value = grades.get(grade) == null ? null : grades.get(grade).trim();
            if (value == null) continue;
            if (!value.isEmpty() && (!TERMS.matcher(value).matches() || Integer.parseInt(value) > 3650)) {
                throw new BadRequestException("terms for grade " + grade + " must be a whole number of days from 0 to 3650");
            }
            gradeTerms.put(grade, value.isEmpty() ? null : Integer.parseInt(value));
        }
        Map<String, Integer> gradeGrace = new LinkedHashMap<>();
        Map<String, String> graces = request == null || request.gradeGrace() == null ? Map.of() : request.gradeGrace();
        for (String grade : gradeOrder(graces.keySet())) {
            String value = graces.get(grade) == null ? null : graces.get(grade).trim();
            if (value == null) continue;
            if (!value.isEmpty() && (!TERMS.matcher(value).matches() || Integer.parseInt(value) > 3650)) {
                throw new BadRequestException("grace days for grade " + grade + " must be a whole number of days from 0 to 3650");
            }
            gradeGrace.put(grade, value.isEmpty() ? null : Integer.parseInt(value));
        }
        Map<String, BigDecimal> gradeProof = new LinkedHashMap<>();
        Map<String, String> proofs = request == null || request.gradeProofThreshold() == null ? Map.of()
                : request.gradeProofThreshold();
        for (String grade : gradeOrder(proofs.keySet())) {
            String value = proofs.get(grade) == null ? null : proofs.get(grade).trim();
            if (value == null) continue;
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new BadRequestException("proof threshold for grade " + grade + " must be 0.00 or more with up to two decimals");
            }
            gradeProof.put(grade, value.isEmpty() ? null : new BigDecimal(value).setScale(2));
        }
        Map<String, BigDecimal> gradeLimit = new LinkedHashMap<>();
        Map<String, String> limits = request == null || request.gradeClerkLimit() == null ? Map.of()
                : request.gradeClerkLimit();
        for (String grade : gradeOrder(limits.keySet())) {
            String value = limits.get(grade) == null ? null : limits.get(grade).trim();
            if (value == null) continue;
            if (!value.isEmpty() && !MONEY.matcher(value).matches()) {
                throw new BadRequestException("clerk limit for grade " + grade + " must be 0.00 or more with up to two decimals");
            }
            gradeLimit.put(grade, value.isEmpty() ? null : new BigDecimal(value).setScale(2));
        }
        if (latePct != null && !MONEY.matcher(latePct).matches()) {
            throw new BadRequestException("late percent must be 0.00 or more with up to two decimals");
        }
        String cap = request == null || request.cap() == null ? null : request.cap().trim();
        if (cap != null && !cap.isEmpty() && !MONEY.matcher(cap).matches()) {
            throw new BadRequestException("cap must be 0.00 or more with up to two decimals");
        }
        String proof = request == null || request.proofThreshold() == null ? null : request.proofThreshold().trim();
        if (proof != null && !proof.isEmpty() && !MONEY.matcher(proof).matches()) {
            throw new BadRequestException("proof threshold must be 0.00 or more with up to two decimals");
        }
        String note = request == null || request.noteThreshold() == null ? null : request.noteThreshold().trim();
        if (note != null && !note.isEmpty() && !MONEY.matcher(note).matches()) {
            throw new BadRequestException("note threshold must be 0.00 or more with up to two decimals");
        }
        String review = request == null || request.reviewThreshold() == null ? null : request.reviewThreshold().trim();
        if (review != null && !review.isEmpty() && !MONEY.matcher(review).matches()) {
            throw new BadRequestException("review threshold must be 0.00 or more with up to two decimals");
        }
        String pot = request == null || request.pot() == null ? null : request.pot().trim();
        if (pot != null && !pot.isEmpty() && !MONEY.matcher(pot).matches()) {
            throw new BadRequestException("pot must be 0.00 or more with up to two decimals");
        }
        String alert = request == null || request.alertThreshold() == null ? null : request.alertThreshold().trim();
        if (alert != null && !alert.isEmpty() && !MONEY.matcher(alert).matches()) {
            throw new BadRequestException("alert threshold must be 0.00 or more with up to two decimals");
        }
        String rounding = request == null || request.rounding() == null ? null : request.rounding().trim();
        if (rounding != null && !rounding.isEmpty() && SettingsRepository.roundingMode(rounding) == null) {
            throw new BadRequestException("rounding mode must be half-up or half-even");
        }
        String parent = request == null || request.parent() == null ? null : request.parent().trim();
        String fiscal = request == null || request.fiscalStartMonth() == null ? null : request.fiscalStartMonth().trim();
        if (fiscal != null && (!FISCAL_MONTH.matcher(fiscal).matches() || Integer.parseInt(fiscal) < 1
                || Integer.parseInt(fiscal) > 12)) {
            throw new BadRequestException("fiscal year start month must be a month from 1 to 12");
        }
        String grace = request == null || request.graceDays() == null ? null : request.graceDays().trim();
        if (grace != null && !grace.isEmpty() && (!TERMS.matcher(grace).matches() || Integer.parseInt(grace) > 3650)) {
            throw new BadRequestException("grace days must be a whole number of days from 0 to 3650");
        }
        String escalate = request == null || request.escalateAfter() == null ? null : request.escalateAfter().trim();
        if (escalate != null && !escalate.isEmpty() && (!TERMS.matcher(escalate).matches() || Integer.parseInt(escalate) > 3650)) {
            throw new BadRequestException("escalation gap must be a whole number of days from 0 to 3650");
        }
        String sla = request == null || request.reviewSla() == null ? null : request.reviewSla().trim();
        if (sla != null && !sla.isEmpty() && (!TERMS.matcher(sla).matches() || Integer.parseInt(sla) > 3650)) {
            throw new BadRequestException("review SLA must be a whole number of days from 0 to 3650");
        }
        String pendingSla = request == null || request.pendingSla() == null ? null : request.pendingSla().trim();
        if (pendingSla != null && !pendingSla.isEmpty() && (!TERMS.matcher(pendingSla).matches() || Integer.parseInt(pendingSla) > 3650)) {
            throw new BadRequestException("pending SLA must be a whole number of days from 0 to 3650");
        }
        String maxOpen = request == null || request.maxOpen() == null ? null : request.maxOpen().trim();
        if (maxOpen != null && !maxOpen.isEmpty() && (!TERMS.matcher(maxOpen).matches() || Integer.parseInt(maxOpen) > 100000)) {
            throw new BadRequestException("max open claims must be a whole number from 0 to 100000");
        }
        String scoreMid = request == null || request.scoreMid() == null ? null : request.scoreMid().trim();
        if (scoreMid != null && !scoreMid.isEmpty() && !SCORE.matcher(scoreMid).matches()) {
            throw new BadRequestException("the mid score threshold must be a whole number");
        }
        String scoreHigh = request == null || request.scoreHigh() == null ? null : request.scoreHigh().trim();
        if (scoreHigh != null && !scoreHigh.isEmpty() && !SCORE.matcher(scoreHigh).matches()) {
            throw new BadRequestException("the high score threshold must be a whole number");
        }
        String scoreLog = request == null || request.scoreLog() == null ? null : request.scoreLog().trim();
        if (scoreLog != null && !scoreLog.isEmpty() && !scoreLog.equals("on")) {
            throw new BadRequestException("the score log must be on or blank");
        }
        String kinds = request == null || request.proofKinds() == null ? null : request.proofKinds().trim();
        if (kinds != null && !kinds.isEmpty()) {
            java.util.List<String> listed = new java.util.ArrayList<>();
            for (String k : kinds.split(",", -1)) {
                String kind = k.trim();
                if (!Claim.PROOF_KINDS.contains(kind)) {
                    throw new BadRequestException("proof kinds must be a comma separated list of receipt and photo");
                }
                if (!listed.contains(kind)) listed.add(kind);
            }
            kinds = String.join(",", listed);
        }
        String defaultGrade = request == null || request.defaultGrade() == null ? null : request.defaultGrade().trim();
        if (defaultGrade != null && !defaultGrade.isEmpty() && !SettingsRepository.GRADE_CODE.matcher(defaultGrade).matches()) {
            throw new BadRequestException("default grade must be one to three capital letters");
        }
        String policy = request == null || request.policyDate() == null ? null : request.policyDate().trim();
        LocalDate policyDate = null;
        if (policy != null && !policy.isEmpty()) {
            try {
                policyDate = LocalDate.parse(policy);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("policy date must be a date (yyyy-mm-dd)");
            }
        }
        String numbering = request == null || request.periodNumberingFrom() == null ? null
                : request.periodNumberingFrom().trim();
        LocalDate numberingFrom = null;
        if (numbering != null && !numbering.isEmpty()) {
            try {
                numberingFrom = LocalDate.parse(numbering);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("period numbering from must be a date (yyyy-mm-dd)");
            }
        }
        String entry = request == null || request.entryDate() == null ? null : request.entryDate().trim();
        LocalDate entryDate = null;
        if (entry != null && !entry.isEmpty()) {
            try {
                entryDate = LocalDate.parse(entry);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("entry date must be a date (yyyy-mm-dd)");
            }
        }
        Map<String, String> permissions = new LinkedHashMap<>();
        Map<String, String> perms = request == null || request.permissions() == null ? Map.of() : request.permissions();
        for (String action : perms.keySet()) {
            if (!SettingsRepository.PERMISSION_ACTIONS.contains(action)) {
                throw new BadRequestException("permissions are for open, settle, void, reassign, close, reopen and today");
            }
        }
        for (String action : SettingsRepository.PERMISSION_ACTIONS) {
            String value = perms.get(action) == null ? null : perms.get(action).trim();
            if (value == null) continue;
            List<String> roles = new java.util.ArrayList<>();
            if (!value.isEmpty()) {
                for (String part : value.split(",", -1)) {
                    String r = part.trim();
                    boolean today = action.equals("today");
                    if (!r.equals(SettingsRepository.CLERK) && !r.equals(SettingsRepository.STEWARD)
                            && !(today && r.equals(SettingsRepository.RESTRICTED))) {
                        throw new BadRequestException("permission for " + action + " must be a comma separated list of roles ("
                                + (today ? "clerk, steward, restricted" : "clerk, steward") + ")");
                    }
                    if (!roles.contains(r)) roles.add(r);
                }
            }
            permissions.put(action, String.join(",", roles));
        }
        Map<String, LocalDate> from = new LinkedHashMap<>();
        Map<String, String> froms = request == null || request.from() == null ? Map.of() : request.from();
        for (Map.Entry<String, String> given : froms.entrySet()) {
            String key = given.getKey();
            String date = given.getValue() == null ? "" : given.getValue().trim();
            if (date.isEmpty()) continue;
            boolean saved = switch (key) {
                case "clerk-limit" -> limit != null;
                case "floor" -> floor != null;
                case "terms" -> terms != null;
                case "late-pct" -> latePct != null;
                case "cap" -> cap != null;
                case "proof-threshold" -> proof != null;
                case "note-threshold" -> note != null;
                case "rounding" -> rounding != null;
                case "review-threshold" -> review != null;
                default -> (key.startsWith("terms-") && gradeTerms.containsKey(key.substring(6)))
                        || (key.startsWith("grace-") && gradeGrace.containsKey(key.substring(6)))
                        || (key.startsWith("proof-threshold-") && gradeProof.containsKey(key.substring(16)))
                        || (key.startsWith("clerk-limit-") && gradeLimit.containsKey(key.substring(12)));
            };
            if (!saved) continue;
            try {
                from.put(key, LocalDate.parse(date));
            } catch (DateTimeParseException e) {
                throw new BadRequestException("from-date for " + key + " must be a date (yyyy-mm-dd)");
            }
        }
        settings.set(new SettingsInput(limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor != null, floor == null ? null : new BigDecimal(floor).setScale(2),
                terms != null, terms == null ? null : Integer.parseInt(terms),
                latePct != null, latePct == null ? null : new BigDecimal(latePct).setScale(2),
                policy != null, policyDate, numbering != null, numberingFrom, gradeTerms,
                cap != null, cap == null || cap.isEmpty() ? null : new BigDecimal(cap).setScale(2), from,
                proof != null, proof == null || proof.isEmpty() ? null : new BigDecimal(proof).setScale(2), permissions,
                grace != null, grace == null || grace.isEmpty() ? null : Integer.parseInt(grace),
                note != null, note == null || note.isEmpty() ? null : new BigDecimal(note).setScale(2),
                entry != null, entryDate, gradeGrace, gradeProof,
                pot != null, pot == null || pot.isEmpty() ? null : new BigDecimal(pot).setScale(2),
                parent != null, parent == null || parent.isEmpty() ? null : parent,
                fiscal != null, fiscal == null ? null : Integer.parseInt(fiscal),
                alert != null, alert == null || alert.isEmpty() ? null : new BigDecimal(alert).setScale(2),
                rounding != null, rounding == null || rounding.isEmpty() ? null : rounding,
                review != null, review == null || review.isEmpty() ? null : new BigDecimal(review).setScale(2),
                escalate != null, escalate == null || escalate.isEmpty() ? null : Integer.parseInt(escalate),
                sla != null, sla == null || sla.isEmpty() ? null : Integer.parseInt(sla),
                kinds != null, kinds == null || kinds.isEmpty() ? null : kinds,
                defaultGrade != null, defaultGrade == null || defaultGrade.isEmpty() ? null : defaultGrade,
                gradeLimit,
                scoreMid != null, scoreMid == null || scoreMid.isEmpty() ? null : Integer.parseInt(scoreMid),
                scoreHigh != null, scoreHigh == null || scoreHigh.isEmpty() ? null : Integer.parseInt(scoreHigh),
                scoreLog != null, scoreLog != null && !scoreLog.isEmpty(),
                maxOpen != null, maxOpen == null || maxOpen.isEmpty() ? null : Integer.parseInt(maxOpen),
                pendingSla != null, pendingSla == null || pendingSla.isEmpty() ? null : Integer.parseInt(pendingSla)));
    }

    /** The grades a per-grade setting names, A, B and C first, then the others in the order given; a name that is not a
     *  grade code is refused. */
    private static List<String> gradeOrder(java.util.Set<String> named) throws BadRequestException {
        List<String> ordered = new java.util.ArrayList<>();
        for (String grade : SettingsRepository.STANDARD_GRADES) {
            if (named.contains(grade)) ordered.add(grade);
        }
        for (String grade : named) {
            if (grade == null || !SettingsRepository.GRADE_CODE.matcher(grade).matches()) {
                throw new BadRequestException("grade must be one to three capital letters");
            }
            if (!ordered.contains(grade)) ordered.add(grade);
        }
        return ordered;
    }
}
