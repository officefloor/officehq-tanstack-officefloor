package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.Role;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    private static final Pattern FIGURE = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Pattern MONTH = Pattern.compile("0?[1-9]|1[0-2]");
    private static final java.util.Set<String> DATED = java.util.Set.of(Settings.CLERK_LIMIT, Settings.FLOOR,
            Settings.TERMS, Settings.LATE_PCT, Settings.CAP, Settings.gradeTermsKey("A"), Settings.gradeTermsKey("B"),
            Settings.gradeTermsKey("C"), Settings.PROOF_THRESHOLD, Settings.NOTE_THRESHOLD, Settings.ROUNDING,
            Settings.gradeGraceKey("A"), Settings.gradeGraceKey("B"), Settings.gradeGraceKey("C"),
            Settings.gradeProofThresholdKey("A"), Settings.gradeProofThresholdKey("B"),
            Settings.gradeProofThresholdKey("C"), Settings.REVIEW_THRESHOLD, Settings.gradeClerkLimitKey("A"),
            Settings.gradeClerkLimitKey("B"), Settings.gradeClerkLimitKey("C"));

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body)
            throws InvalidSettingException {
        boolean limitChanged = request != null && request.clerkLimit() != null;
        BigDecimal limit = null;
        if (limitChanged && !request.clerkLimit().trim().isEmpty()) {
            String value = request.clerkLimit().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A clerk limit is 0.00 or more, with up to two decimals");
            }
            limit = new BigDecimal(value).setScale(2);
        }
        BigDecimal floor = null;
        if (request != null && request.floor() != null) {
            String value = request.floor().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A floor is 0.00 or more, with up to two decimals");
            }
            floor = new BigDecimal(value).setScale(2);
        }
        Integer terms = null;
        if (request != null && request.terms() != null) {
            String value = request.terms().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("Terms are a whole number of days, 0 or more");
            }
            terms = Integer.valueOf(value);
        }
        BigDecimal latePct = null;
        if (request != null && request.latePct() != null) {
            String value = request.latePct().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A late percent is 0.00 or more, with up to two decimals");
            }
            latePct = new BigDecimal(value).setScale(2);
        }
        boolean policyChanged = request != null && request.policyDate() != null;
        LocalDate policyDate = null;
        if (policyChanged && !request.policyDate().trim().isEmpty()) {
            try {
                policyDate = LocalDate.parse(request.policyDate().trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("A policy date is a date, yyyy-MM-dd, or blank");
            }
        }
        boolean numberingChanged = request != null && request.periodNumberingFrom() != null;
        LocalDate numberingFrom = null;
        if (numberingChanged && !request.periodNumberingFrom().trim().isEmpty()) {
            try {
                numberingFrom = LocalDate.parse(request.periodNumberingFrom().trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("A period numbering from is a date, yyyy-MM-dd, or blank");
            }
        }
        Map<String, Integer> gradeTerms = new LinkedHashMap<>();
        if (request != null && request.gradeTerms() != null) {
            for (String grade : request.gradeTerms().keySet()) {
                if (!Settings.isGradeCode(grade)) throw new InvalidSettingException("A grade is one to three capital letters");
            }
            for (String grade : request.gradeTerms().keySet()) {
                String value = request.gradeTerms().get(grade);
                if (value == null) continue;
                value = value.trim();
                if (value.isEmpty()) {
                    gradeTerms.put(grade, null);
                } else if (!DAYS.matcher(value).matches()) {
                    throw new InvalidSettingException("Terms are a whole number of days, 0 or more, or blank");
                } else {
                    gradeTerms.put(grade, Integer.valueOf(value));
                }
            }
        }
        Map<String, Integer> gradeGrace = new LinkedHashMap<>();
        if (request != null && request.gradeGrace() != null) {
            for (String grade : request.gradeGrace().keySet()) {
                if (!Settings.isGradeCode(grade)) throw new InvalidSettingException("A grade is one to three capital letters");
            }
            for (String grade : request.gradeGrace().keySet()) {
                String value = request.gradeGrace().get(grade);
                if (value == null) continue;
                value = value.trim();
                if (value.isEmpty()) {
                    gradeGrace.put(grade, null);
                } else if (!DAYS.matcher(value).matches()) {
                    throw new InvalidSettingException("Grace days are a whole number of days, 0 or more, or blank");
                } else {
                    gradeGrace.put(grade, Integer.valueOf(value));
                }
            }
        }
        Map<String, BigDecimal> gradeProofThreshold = new LinkedHashMap<>();
        if (request != null && request.gradeProofThreshold() != null) {
            for (String grade : request.gradeProofThreshold().keySet()) {
                if (!Settings.isGradeCode(grade)) throw new InvalidSettingException("A grade is one to three capital letters");
            }
            for (String grade : request.gradeProofThreshold().keySet()) {
                String value = request.gradeProofThreshold().get(grade);
                if (value == null) continue;
                value = value.trim();
                if (value.isEmpty()) {
                    gradeProofThreshold.put(grade, null);
                } else if (!FIGURE.matcher(value).matches()) {
                    throw new InvalidSettingException("A proof threshold is 0.00 or more, with up to two decimals, or blank");
                } else {
                    gradeProofThreshold.put(grade, new BigDecimal(value).setScale(2));
                }
            }
        }
        Map<String, BigDecimal> gradeClerkLimit = new LinkedHashMap<>();
        if (request != null && request.gradeClerkLimit() != null) {
            for (String grade : request.gradeClerkLimit().keySet()) {
                if (!Settings.isGradeCode(grade)) throw new InvalidSettingException("A grade is one to three capital letters");
            }
            for (String grade : request.gradeClerkLimit().keySet()) {
                String value = request.gradeClerkLimit().get(grade);
                if (value == null) continue;
                value = value.trim();
                if (value.isEmpty()) {
                    gradeClerkLimit.put(grade, null);
                } else if (!FIGURE.matcher(value).matches()) {
                    throw new InvalidSettingException("A clerk limit is 0.00 or more, with up to two decimals, or blank");
                } else {
                    gradeClerkLimit.put(grade, new BigDecimal(value).setScale(2));
                }
            }
        }
        boolean capChanged = request != null && request.cap() != null;
        BigDecimal cap = null;
        if (capChanged && !request.cap().trim().isEmpty()) {
            String value = request.cap().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A cap is 0.00 or more, with up to two decimals, or blank");
            }
            cap = new BigDecimal(value).setScale(2);
        }
        boolean thresholdChanged = request != null && request.proofThreshold() != null;
        BigDecimal threshold = null;
        if (thresholdChanged && !request.proofThreshold().trim().isEmpty()) {
            String value = request.proofThreshold().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A proof threshold is 0.00 or more, with up to two decimals, or blank");
            }
            threshold = new BigDecimal(value).setScale(2);
        }
        boolean noteThresholdChanged = request != null && request.noteThreshold() != null;
        BigDecimal noteThreshold = null;
        if (noteThresholdChanged && !request.noteThreshold().trim().isEmpty()) {
            String value = request.noteThreshold().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A note threshold is 0.00 or more, with up to two decimals, or blank");
            }
            noteThreshold = new BigDecimal(value).setScale(2);
        }
        boolean roundingChanged = request != null && request.rounding() != null;
        String rounding = null;
        if (roundingChanged && !request.rounding().trim().isEmpty()) {
            String value = request.rounding().trim();
            if (!Settings.HALF_UP.equals(value) && !Settings.HALF_EVEN.equals(value)) {
                throw new InvalidSettingException("A rounding mode is half-up or half-even, or blank");
            }
            rounding = value;
        }
        boolean reviewThresholdChanged = request != null && request.reviewThreshold() != null;
        BigDecimal reviewThreshold = null;
        if (reviewThresholdChanged && !request.reviewThreshold().trim().isEmpty()) {
            String value = request.reviewThreshold().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A review threshold is 0.00 or more, with up to two decimals, or blank");
            }
            reviewThreshold = new BigDecimal(value).setScale(2);
        }
        Map<String, String> perms = new LinkedHashMap<>();
        if (request != null && request.perms() != null) {
            for (String action : request.perms().keySet()) {
                if (!Settings.ACTIONS.contains(action)) {
                    throw new InvalidSettingException("An action is one of " + String.join(", ", Settings.ACTIONS));
                }
            }
            for (String action : Settings.ACTIONS) {
                String value = request.perms().get(action);
                if (value == null) continue;
                value = value.trim();
                java.util.List<String> roles = new java.util.ArrayList<>();
                if (!value.isEmpty()) {
                    for (String role : value.split(",", -1)) {
                        role = role.trim();
                        if (!Role.CLERK.equals(role) && !Role.STEWARD.equals(role)) {
                            throw new InvalidSettingException("Roles are clerk or steward, separated by commas, or blank");
                        }
                        if (!roles.contains(role)) roles.add(role);
                    }
                }
                perms.put(action, String.join(",", roles));
            }
        }
        boolean graceChanged = request != null && request.graceDays() != null;
        Integer graceDays = null;
        if (graceChanged && !request.graceDays().trim().isEmpty()) {
            String value = request.graceDays().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("Grace days are a whole number of days, 0 or more, or blank");
            }
            graceDays = Integer.valueOf(value);
        }
        boolean entryChanged = request != null && request.entryDate() != null;
        LocalDate entryDate = null;
        if (entryChanged && !request.entryDate().trim().isEmpty()) {
            try {
                entryDate = LocalDate.parse(request.entryDate().trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("An entry date is a date, yyyy-MM-dd, or blank");
            }
        }
        boolean potChanged = request != null && request.pot() != null;
        BigDecimal pot = null;
        if (potChanged && !request.pot().trim().isEmpty()) {
            String value = request.pot().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A pot is 0.00 or more, with up to two decimals, or blank");
            }
            pot = new BigDecimal(value).setScale(2);
        }
        boolean parentChanged = request != null && request.parent() != null;
        String parent = parentChanged && !request.parent().trim().isEmpty() ? request.parent().trim() : null;
        boolean fiscalChanged = request != null && request.fiscalStartMonth() != null;
        Integer fiscalStartMonth = null;
        if (fiscalChanged) {
            String value = request.fiscalStartMonth().trim();
            if (!MONTH.matcher(value).matches()) {
                throw new InvalidSettingException("A fiscal start month is a month, 1 to 12");
            }
            fiscalStartMonth = Integer.valueOf(value);
        }
        boolean alertChanged = request != null && request.alertThreshold() != null;
        BigDecimal alertThreshold = null;
        if (alertChanged && !request.alertThreshold().trim().isEmpty()) {
            String value = request.alertThreshold().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("An alert threshold is 0.00 or more, with up to two decimals, or blank");
            }
            alertThreshold = new BigDecimal(value).setScale(2);
        }
        boolean escalateChanged = request != null && request.escalateAfter() != null;
        Integer escalateAfter = null;
        if (escalateChanged && !request.escalateAfter().trim().isEmpty()) {
            String value = request.escalateAfter().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("An escalation gap is a whole number of days, 0 or more, or blank");
            }
            escalateAfter = Integer.valueOf(value);
        }
        boolean reviewSlaChanged = request != null && request.reviewSla() != null;
        Integer reviewSla = null;
        if (reviewSlaChanged && !request.reviewSla().trim().isEmpty()) {
            String value = request.reviewSla().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("A review SLA is a whole number of days, 0 or more, or blank");
            }
            reviewSla = Integer.valueOf(value);
        }
        boolean proofKindsChanged = request != null && request.proofKinds() != null;
        String proofKinds = null;
        if (proofKindsChanged && !request.proofKinds().trim().isEmpty()) {
            java.util.List<String> kinds = new java.util.ArrayList<>();
            for (String kind : request.proofKinds().split(",", -1)) {
                kind = kind.trim();
                if (!net.officefloor.hq.app.claim.ProofKind.KINDS.contains(kind)) {
                    throw new InvalidSettingException("Proof kinds are receipt or photo, separated by commas, or blank");
                }
                if (!kinds.contains(kind)) kinds.add(kind);
            }
            proofKinds = String.join(",", kinds);
        }
        boolean defaultGradeChanged = request != null && request.defaultGrade() != null;
        String defaultGrade = null;
        if (defaultGradeChanged && !request.defaultGrade().trim().isEmpty()) {
            defaultGrade = request.defaultGrade().trim();
            if (!Settings.isGradeCode(defaultGrade)) {
                throw new InvalidSettingException("A default grade is one to three capital letters, or blank");
            }
        }
        boolean scoreMidChanged = request != null && request.scoreMid() != null;
        Integer scoreMid = scoreMidChanged ? score(request.scoreMid(), "mid") : null;
        boolean scoreHighChanged = request != null && request.scoreHigh() != null;
        Integer scoreHigh = scoreHighChanged ? score(request.scoreHigh(), "high") : null;
        boolean scoreLogChanged = request != null && request.scoreLog() != null;
        boolean scoreLog = false;
        if (scoreLogChanged && !request.scoreLog().trim().isEmpty()) {
            if (!Settings.ON.equals(request.scoreLog().trim())) {
                throw new InvalidSettingException("The score log is on, or blank");
            }
            scoreLog = true;
        }
        boolean maxOpenChanged = request != null && request.maxOpen() != null;
        Integer maxOpen = null;
        if (maxOpenChanged && !request.maxOpen().trim().isEmpty()) {
            String value = request.maxOpen().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("A maximum of open claims is a whole number, 0 or more, or blank");
            }
            maxOpen = Integer.valueOf(value);
        }
        boolean pendingSlaChanged = request != null && request.pendingSla() != null;
        Integer pendingSla = null;
        if (pendingSlaChanged && !request.pendingSla().trim().isEmpty()) {
            String value = request.pendingSla().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("A pending SLA is a whole number of days, 0 or more, or blank");
            }
            pendingSla = Integer.valueOf(value);
        }
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (request != null && request.from() != null) {
            for (Map.Entry<String, String> entry : request.from().entrySet()) {
                if (!DATED.contains(entry.getKey())) throw new InvalidSettingException("Only a figure setting has a from-date");
                String value = entry.getValue() == null ? "" : entry.getValue().trim();
                if (value.isEmpty()) continue;
                try {
                    from.put(entry.getKey(), LocalDate.parse(value));
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("A from-date is a date, yyyy-MM-dd, or blank");
                }
            }
        }
        body.set(new SettingsInput(limitChanged, limit, floor, terms, latePct, policyChanged, policyDate,
                numberingChanged, numberingFrom, gradeTerms, capChanged, cap, from,
                thresholdChanged, threshold, perms, graceChanged, graceDays, noteThresholdChanged, noteThreshold,
                entryChanged, entryDate, gradeGrace, gradeProofThreshold, potChanged, pot, parentChanged, parent,
                fiscalChanged, fiscalStartMonth, alertChanged, alertThreshold, roundingChanged, rounding,
                reviewThresholdChanged, reviewThreshold, escalateChanged, escalateAfter, reviewSlaChanged, reviewSla,
                proofKindsChanged, proofKinds, defaultGradeChanged, defaultGrade,
                scoreMidChanged, scoreMid, scoreHighChanged, scoreHigh, scoreLogChanged, scoreLog, gradeClerkLimit,
                maxOpenChanged, maxOpen, pendingSlaChanged, pendingSla));
    }

    /** A score threshold: a whole number (null for blank). */
    private static Integer score(String given, String which) throws InvalidSettingException {
        String value = given.trim();
        if (value.isEmpty()) return null;
        if (!SCORE.matcher(value).matches()) {
            throw new InvalidSettingException("A " + which + " score threshold is a whole number, or blank");
        }
        return Integer.valueOf(value);
    }

    private static final Pattern SCORE = Pattern.compile("-?\\d{1,9}");
}
