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
    private static final java.util.Set<String> DATED = java.util.Set.of(Settings.CLERK_LIMIT, Settings.FLOOR,
            Settings.TERMS, Settings.LATE_PCT, Settings.CAP, Settings.gradeTermsKey("A"), Settings.gradeTermsKey("B"),
            Settings.gradeTermsKey("C"), Settings.PROOF_THRESHOLD, Settings.NOTE_THRESHOLD);

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
                if (!Settings.GRADES.contains(grade)) throw new InvalidSettingException("A grade is one of A, B or C");
            }
            for (String grade : Settings.GRADES) {
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
                thresholdChanged, threshold, perms, graceChanged, graceDays, noteThresholdChanged, noteThreshold));
    }
}
