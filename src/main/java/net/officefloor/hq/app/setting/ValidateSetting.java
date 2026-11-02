package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ProofKind;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.sheet.SheetRegistry;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    private static final Pattern LIMIT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");
    private static final Pattern SCORE = Pattern.compile("-?\\d{1,7}");
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody SettingRequest request, Settings settings,
            SheetRegistry registry, Grades grades, Out<SettingInput> body) throws InvalidSettingException {
        SettingInput input = input(request);
        if (Settings.DEFAULT_GRADE.equals(input.key()) && !input.value().isEmpty()
                && !grades.isGrade(sheet, input.value())) {
            throw new InvalidSettingException("The default grade must be one of the sheet's grades, or blank");
        }
        if (Settings.PARENT.equals(input.key())) {
            parent(sheet, input.value(), settings, registry);
        }
        // A dated key may be given the date it is in force from; blank: the sheet's today.
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (Settings.dated(input.key()) && !from.isEmpty()) {
            try {
                input = input.withFrom(LocalDate.parse(from));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be a date (yyyy-mm-dd), or blank");
            }
        }
        body.set(input);
    }

    /** A parent must be a sheet that exists, not this sheet, nor one that has this sheet as an ancestor; blank: none. */
    private static void parent(String sheet, String parent, Settings settings, SheetRegistry registry)
            throws InvalidSettingException {
        if (parent.isEmpty()) {
            return;
        }
        if (parent.equals(sheet)) {
            throw new InvalidSettingException("A sheet cannot be its own parent");
        }
        if (!registry.exists(parent)) {
            throw new InvalidSettingException("No such sheet: " + parent);
        }
        Set<String> seen = new HashSet<>();
        for (String up = settings.parent(parent); !up.isEmpty() && seen.add(up); up = settings.parent(up)) {
            if (up.equals(sheet)) {
                throw new InvalidSettingException("The sheet " + parent + " has this sheet as an ancestor");
            }
        }
    }

    private SettingInput input(SettingRequest request) throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (!Settings.CLERK_LIMIT.equals(key) && !Settings.CAP.equals(key) && !Settings.PROOF_THRESHOLD.equals(key) && !Settings.PROOF_KINDS.equals(key) && !Settings.NOTE_THRESHOLD.equals(key) && !Settings.REVIEW_THRESHOLD.equals(key) && !Settings.FLOOR.equals(key) && !Settings.TERMS.equals(key)
                && !Settings.LATE_PCT.equals(key) && !Settings.POLICY_DATE.equals(key)
                && !Settings.PERIOD_NUMBERING_FROM.equals(key) && !Settings.GRACE_DAYS.equals(key) && !Settings.ESCALATE_AFTER.equals(key) && !Settings.REVIEW_SLA.equals(key) && !Settings.PENDING_SLA.equals(key) && !Settings.MAX_OPEN.equals(key) && !Settings.ENTRY_DATE.equals(key)
                && !Settings.POT.equals(key) && !Settings.ALERT_THRESHOLD.equals(key) && !Settings.SCORE_MID.equals(key) && !Settings.SCORE_HIGH.equals(key) && !Settings.SCORE_LOG.equals(key) && !Settings.PARENT.equals(key) && !Settings.FISCAL_START_MONTH.equals(key) && !Settings.ROUNDING.equals(key) && !Settings.DEFAULT_GRADE.equals(key)
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeTerms(g).equals(key))
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeGrace(g).equals(key))
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeProofThreshold(g).equals(key))
                && Settings.GRADES.stream().noneMatch((g) -> Settings.gradeClerkLimit(g).equals(key))
                && Settings.PERMISSIONS.stream().noneMatch((a) -> Settings.perm(a).equals(key))) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        // An action's permitted roles are a comma separated list of clerk and steward; blank keeps its standing rule.
        if (key.startsWith("perm-")) {
            List<String> roles = new ArrayList<>();
            for (String role : value.isEmpty() ? new String[0] : value.split(",")) {
                role = role.trim();
                if (!Role.CLERK.equals(role) && !Role.STEWARD.equals(role)) {
                    throw new InvalidSettingException("The roles allowed an action must be clerk or steward, comma separated, or blank");
                }
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
            return new SettingInput(key, String.join(",", roles));
        }
        // The proof kinds are a comma separated list of receipt and photo; blank means none.
        if (Settings.PROOF_KINDS.equals(key)) {
            List<String> kinds = new ArrayList<>();
            for (String kind : value.isEmpty() ? new String[0] : value.split(",")) {
                kind = kind.trim();
                if (!ProofKind.ALL.contains(kind)) {
                    throw new InvalidSettingException("The proof kinds must be receipt or photo, comma separated, or blank");
                }
                if (!kinds.contains(kind)) {
                    kinds.add(kind);
                }
            }
            return new SettingInput(key, String.join(",", kinds));
        }
        // The policy date is a date; blank follows the sheet's today.
        if (Settings.POLICY_DATE.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The policy date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The rounding mode is half-up or half-even; blank means none, so the policy date decides.
        if (Settings.ROUNDING.equals(key)) {
            if (!value.isEmpty() && !Settings.ROUNDING_MODES.contains(value)) {
                throw new InvalidSettingException("The rounding mode must be half-up or half-even, or blank");
            }
            return new SettingInput(key, value);
        }
        // The period numbering from-date is a date; blank turns it off.
        if (Settings.PERIOD_NUMBERING_FROM.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The period numbering from-date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The entry date is a date; blank turns it off, so a new claim takes the sheet's today.
        if (Settings.ENTRY_DATE.equals(key)) {
            if (!value.isEmpty()) {
                try {
                    value = LocalDate.parse(value).toString();
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The entry date must be a date (yyyy-mm-dd), or blank");
                }
            }
            return new SettingInput(key, value);
        }
        // The fiscal start month is a month, 1 to 12; anything else (blank too) is refused.
        if (Settings.FISCAL_START_MONTH.equals(key)) {
            int month = DAYS.matcher(value).matches() ? Integer.parseInt(value) : 0;
            if (month < 1 || month > 12) {
                throw new InvalidSettingException("The fiscal start month must be a month, 1 to 12");
            }
            return new SettingInput(key, Integer.toString(month));
        }
        // The default grade is one of the grades; blank means A.
        if (Settings.DEFAULT_GRADE.equals(key)) {
            if (!value.isEmpty() && !value.matches("[A-Z]{1,3}")) {
                throw new InvalidSettingException("The default grade must be one of the sheet's grades, or blank");
            }
            return new SettingInput(key, value);
        }
        // The parent is a sheet id, checked against the sheets by the caller; blank means none.
        if (Settings.PARENT.equals(key)) {
            return new SettingInput(key, value);
        }
        // The grace days are a whole number of days; blank turns the auto close off.
        if (Settings.GRACE_DAYS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The escalation gap is a whole number of days; blank turns escalation off.
        if (Settings.ESCALATE_AFTER.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The escalation gap must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The review SLA is a whole number of days; blank means none.
        if (Settings.REVIEW_SLA.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The review SLA must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The pending SLA is a whole number of days; blank means none.
        if (Settings.PENDING_SLA.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The pending SLA must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The maximum of open claims per person is a whole number; blank means none.
        if (Settings.MAX_OPEN.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The maximum of open claims must be a whole number, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // A score threshold is a whole number (a score may be below 0); blank means no bands.
        if (Settings.SCORE_MID.equals(key) || Settings.SCORE_HIGH.equals(key)) {
            if (!value.isEmpty() && !SCORE.matcher(value).matches()) {
                throw new InvalidSettingException("The " + key + " must be a whole number, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The score log is on or blank (off).
        if (Settings.SCORE_LOG.equals(key)) {
            if (!value.isEmpty() && !"on".equals(value)) {
                throw new InvalidSettingException("The score log must be on, or blank");
            }
            return new SettingInput(key, value);
        }
        // A grade's grace is a whole number of days; blank means none.
        if (Settings.GRADES.stream().anyMatch((g) -> Settings.gradeGrace(g).equals(key))) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace of a grade must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        // The terms are a whole number of days; blank takes the default.
        if (Settings.TERMS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, Integer.toString(Integer.parseInt(value.isEmpty() ? Settings.DEFAULT_TERMS : value)));
        }
        // A grade's own terms are a whole number of days; blank clears them, so the grade takes the sheet's terms.
        if (key.startsWith(Settings.TERMS + "-")) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms of a grade must be a whole number of days, 0 or more, or blank");
            }
            return new SettingInput(key, value.isEmpty() ? "" : Integer.toString(Integer.parseInt(value)));
        }
        if (!value.isEmpty() && !LIMIT.matcher(value).matches()) {
            throw new InvalidSettingException("The " + key + " must be 0.00 or more with up to two decimals, or blank");
        }
        // A blank floor or late percent takes its default: they always have a value.
        if (Settings.FLOOR.equals(key) && value.isEmpty()) {
            value = Settings.DEFAULT_FLOOR;
        }
        if (Settings.LATE_PCT.equals(key) && value.isEmpty()) {
            value = Settings.DEFAULT_LATE_PCT;
        }
        return new SettingInput(key,
                value.isEmpty() ? "" : new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }
}
