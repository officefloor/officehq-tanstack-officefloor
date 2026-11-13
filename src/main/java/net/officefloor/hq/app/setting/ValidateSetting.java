package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern LIMIT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    /** A whole number of days, 0 or more. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    /** A whole number, which may be negative. */
    private static final Pattern SCORE = Pattern.compile("-?\\d{1,7}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingRequest request, Out<SettingInput> body, RoleStore roles,
            SettingStore settings, SheetStore sheets) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String key = request == null || request.key() == null ? "" : request.key().trim();
        boolean floor = SettingStore.FLOOR.equals(key);
        boolean terms = SettingStore.TERMS.equals(key);
        boolean gradeTerms = key.startsWith(SettingStore.TERMS_PREFIX)
                && SettingStore.GRADES.contains(key.substring(SettingStore.TERMS_PREFIX.length()));
        boolean gradeGrace = SettingStore.isGradeGrace(key);
        boolean late = SettingStore.LATE_PCT.equals(key);
        boolean numbering = SettingStore.PERIOD_NUMBERING_FROM.equals(key);
        boolean entry = SettingStore.ENTRY_DATE.equals(key);
        boolean dated = SettingStore.POLICY_DATE.equals(key) || numbering || entry;
        boolean cap = SettingStore.CAP.equals(key);
        boolean gradeProof = SettingStore.isGradeProofThreshold(key);
        boolean proof = SettingStore.PROOF_THRESHOLD.equals(key) || gradeProof;
        boolean gradeClerk = SettingStore.isGradeClerkLimit(key);
        boolean note = SettingStore.NOTE_THRESHOLD.equals(key);
        boolean review = SettingStore.REVIEW_THRESHOLD.equals(key);
        boolean pot = SettingStore.POT.equals(key);
        boolean alert = SettingStore.ALERT_THRESHOLD.equals(key);
        if (SettingStore.GRACE_DAYS.equals(key) || SettingStore.ESCALATE_AFTER.equals(key)
                || SettingStore.REVIEW_SLA.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSheetException((SettingStore.GRACE_DAYS.equals(key) ? "The grace days"
                        : SettingStore.REVIEW_SLA.equals(key) ? "The review SLA" : "The escalation gap")
                        + " must be a whole number of days, 0 or more, or blank");
            }
            body.set(new SettingInput(key, value.isEmpty() ? null : String.valueOf(Integer.parseInt(value)), null,
                    request.user()));
            return;
        }
        if (SettingStore.SCORE_MID.equals(key) || SettingStore.SCORE_HIGH.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            if (!value.isEmpty() && !SCORE.matcher(value).matches()) {
                throw new InvalidSheetException((SettingStore.SCORE_MID.equals(key) ? "The mid" : "The high")
                        + " score threshold must be a whole number, or blank");
            }
            body.set(new SettingInput(key, value.isEmpty() ? null : String.valueOf(Integer.parseInt(value)), null,
                    request.user()));
            return;
        }
        if (SettingStore.SCORE_LOG.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim().toLowerCase();
            if (!value.isEmpty() && !"on".equals(value)) {
                throw new InvalidSheetException("The score log must be on, or blank");
            }
            body.set(new SettingInput(key, value.isEmpty() ? null : value, null, request.user()));
            return;
        }
        if (SettingStore.FISCAL_START_MONTH.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            int month = value.matches("\\d{1,2}") ? Integer.parseInt(value) : 0;
            if (month < 1 || month > 12) {
                throw new InvalidSheetException("The fiscal start month must be a month from 1 to 12");
            }
            body.set(new SettingInput(key, String.valueOf(month), null, request.user()));
            return;
        }
        if (SettingStore.DEFAULT_GRADE.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            if (!value.isEmpty() && !SettingStore.GRADES.contains(value)) {
                throw new InvalidSheetException("The default grade must be A, B or C, or blank");
            }
            body.set(new SettingInput(key, value.isEmpty() ? null : value, null, request.user()));
            return;
        }
        if (SettingStore.PROOF_KINDS.equals(key)) {
            body.set(new SettingInput(key, kinds(request.value()), null, request.user()));
            return;
        }
        if (SettingStore.PARENT.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, null, request.user()));
                return;
            }
            if (value.equals(sheetId.trim())) {
                throw new InvalidSheetException("A sheet cannot be its own parent");
            }
            if (!sheets.exists(value)) {
                throw new InvalidSheetException("The parent must be a sheet that exists: " + value);
            }
            if (settings.cyclic(sheetId.trim(), value)) {
                throw new InvalidSheetException("The parent cannot be a sheet that has this sheet as an ancestor");
            }
            body.set(new SettingInput(key, value, null, request.user()));
            return;
        }
        if (key.startsWith(SettingStore.PERM_PREFIX)
                && SettingStore.PERM_ACTIONS.contains(key.substring(SettingStore.PERM_PREFIX.length()))) {
            body.set(new SettingInput(key, perm(request.value()), null, request.user()));
            return;
        }
        if (SettingStore.ROUNDING.equals(key)) {
            String value = request.value() == null ? "" : request.value().trim();
            String fromValue = request.from() == null ? "" : request.from().trim();
            if (!value.isEmpty() && !SettingStore.ROUNDING_HALF_UP.equals(value)
                    && !SettingStore.ROUNDING_HALF_EVEN.equals(value)) {
                throw new InvalidSheetException("The rounding must be half-up or half-even, or blank");
            }
            LocalDate from = null;
            if (!fromValue.isEmpty()) {
                try {
                    from = LocalDate.parse(fromValue);
                } catch (DateTimeParseException e) {
                    throw new InvalidSheetException("The from-date must be a date (YYYY-MM-DD), or blank");
                }
            }
            body.set(new SettingInput(key, value.isEmpty() ? null : value, from, request.user()));
            return;
        }
        if (!SettingStore.CLERK_LIMIT.equals(key) && !gradeClerk && !cap && !proof && !note && !review && !pot && !alert && !floor && !terms && !gradeTerms && !gradeGrace && !late && !dated) {
            throw new InvalidSheetException("Not a setting: " + key);
        }
        String value = request.value() == null ? "" : request.value().trim();
        String fromValue = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!dated && !pot && !alert && !fromValue.isEmpty()) {
            try {
                from = LocalDate.parse(fromValue);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetException("The from-date must be a date (YYYY-MM-DD), or blank");
            }
        }
        if (dated) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, from, request.user()));
                return;
            }
            try {
                body.set(new SettingInput(key, LocalDate.parse(value).toString(), from, request.user()));
            } catch (DateTimeParseException e) {
                throw new InvalidSheetException((numbering ? "The period numbering from" : entry ? "The entry date" : "The policy date")
                        + " must be a date (YYYY-MM-DD), or blank");
            }
            return;
        }
        if (gradeGrace) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, from, request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The grade's grace days must be a whole number of days, 0 or more, or blank");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), from, request.user()));
            return;
        }
        if (gradeTerms) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, null, from, request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The grade's terms must be a whole number of days, 0 or more, or blank");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), from, request.user()));
            return;
        }
        if (terms) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, String.valueOf(SettingStore.DEFAULT_TERMS), from, request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The terms must be a whole number of days, 0 or more");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), from, request.user()));
            return;
        }
        if (value.isEmpty() && floor) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_FLOOR.toPlainString(), from, request.user()));
            return;
        }
        if (value.isEmpty() && late) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_LATE_PCT.toPlainString(), from, request.user()));
            return;
        }
        if (value.isEmpty()) {
            body.set(new SettingInput(key, null, from, request.user()));
            return;
        }
        if (!LIMIT.matcher(value).matches()) {
            throw new InvalidSheetException(floor ? "The floor must be 0.00 or more with up to two decimals"
                    : late ? "The late percent must be 0.00 or more with up to two decimals"
                    : gradeProof ? "The grade's proof threshold must be 0.00 or more with up to two decimals, or blank"
                    : proof ? "The proof threshold must be 0.00 or more with up to two decimals, or blank"
                    : note ? "The note threshold must be 0.00 or more with up to two decimals, or blank"
                    : review ? "The review threshold must be 0.00 or more with up to two decimals, or blank"
                    : alert ? "The alert threshold must be 0.00 or more with up to two decimals, or blank"
                    : pot ? "The pot must be 0.00 or more with up to two decimals, or blank"
                    : gradeClerk ? "The grade's clerk limit must be 0.00 or more with up to two decimals, or blank"
                    : cap ? "The cap must be 0.00 or more with up to two decimals, or blank"
                    : "The clerk limit must be 0.00 or more with up to two decimals, or blank");
        }
        body.set(new SettingInput(key, new BigDecimal(value).setScale(2).toPlainString(), from, request.user()));
    }

    /** The proof kinds listed (receipt, photo), comma separated without spaces, each once, or null when blank. */
    private static String kinds(String raw) throws InvalidSheetException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        List<String> kinds = new ArrayList<>();
        for (String part : value.split(",")) {
            String kind = part.trim();
            if (!SettingStore.KINDS.contains(kind)) {
                throw new InvalidSheetException("The proof kinds list kinds (receipt, photo), comma separated, or are blank");
            }
            if (!kinds.contains(kind)) {
                kinds.add(kind);
            }
        }
        return String.join(",", kinds);
    }

    /** The roles listed, comma separated without spaces, or null when blank (the standing rule). */
    private static String perm(String raw) throws InvalidSheetException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        List<String> roles = new ArrayList<>();
        for (String part : value.split(",")) {
            String role = part.trim();
            if (Role.of(role) == null || Role.of(role) == Role.VIEWER || Role.of(role) == Role.RESTRICTED) {
                throw new InvalidSheetException("A permission lists roles (clerk, steward), comma separated, or is blank");
            }
            if (!roles.contains(role)) {
                roles.add(role);
            }
        }
        return String.join(",", roles);
    }
}
