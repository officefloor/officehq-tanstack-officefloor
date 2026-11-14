package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ProofKind;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettingsRequest {

    /** 0.00 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?|\\.\\d{1,2}");

    /** A percent: 0.00 or more, with at most two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?|\\.\\d{1,2}");

    /** The grades terms can be set for. */
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** 0 or more whole days. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody @Valid SettingsRequest request, Out<ClerkLimit> limit, Out<Floor> floor,
            Out<Terms> terms, Out<LatePct> latePct, Out<PolicyDate> policyDate,
            Out<PeriodNumberingFrom> periodNumberingFrom, Out<EntryDate> entryDate, Out<GradeTerms> gradeTerms, Out<Cap> cap,
            Out<ProofThreshold> proofThreshold, Out<NoteThreshold> noteThreshold, Out<ReviewThreshold> reviewThreshold, Out<PermissionChanges> permissions, Out<GraceDays> graceDays,
            Out<GradeGrace> gradeGrace, Out<GradeProofThreshold> gradeProofThreshold, Out<Pot> pot, Out<Parent> parent,
            Out<FiscalStartMonth> fiscalStartMonth, Out<AlertThreshold> alertThreshold, Out<RoundingSetting> rounding,
            Out<EscalateAfter> escalateAfter, Out<ReviewSla> reviewSla, Out<ProofKinds> proofKinds,
            Out<DefaultGrade> defaultGrade, Out<GradeClerkLimit> gradeClerkLimit)
            throws InvalidSettingException {
        limit.set(clerkLimit(request.clerkLimit(), from(request.clerkLimitFrom(), "clerk limit")));
        floor.set(floor(request.floor(), from(request.floorFrom(), "floor")));
        terms.set(terms(request.terms(), from(request.termsFrom(), "terms")));
        latePct.set(latePct(request.latePct(), from(request.latePctFrom(), "late percent")));
        policyDate.set(policyDate(request.policyDate()));
        periodNumberingFrom.set(periodNumberingFrom(request.periodNumberingFrom()));
        entryDate.set(entryDate(request.entryDate()));
        gradeTerms.set(gradeTerms(request.gradeTerms(), request.gradeTermsFrom()));
        cap.set(cap(request.cap(), from(request.capFrom(), "cap")));
        proofThreshold.set(proofThreshold(request.proofThreshold(), from(request.proofThresholdFrom(), "proof threshold")));
        noteThreshold.set(noteThreshold(request.noteThreshold(), from(request.noteThresholdFrom(), "note threshold")));
        reviewThreshold.set(reviewThreshold(request.reviewThreshold(), from(request.reviewThresholdFrom(), "review threshold")));
        permissions.set(permissions(request.permissions()));
        graceDays.set(graceDays(request.graceDays()));
        gradeGrace.set(gradeGrace(request.gradeGrace(), request.gradeGraceFrom()));
        gradeProofThreshold.set(gradeProofThreshold(request.gradeProofThreshold(), request.gradeProofThresholdFrom()));
        pot.set(pot(request.pot()));
        parent.set(parent(request.parent()));
        fiscalStartMonth.set(fiscalStartMonth(request.fiscalStartMonth()));
        alertThreshold.set(alertThreshold(request.alertThreshold()));
        rounding.set(rounding(request.rounding(), from(request.roundingFrom(), "rounding")));
        escalateAfter.set(escalateAfter(request.escalateAfter()));
        reviewSla.set(reviewSla(request.reviewSla()));
        proofKinds.set(proofKinds(request.proofKinds()));
        defaultGrade.set(defaultGrade(request.defaultGrade()));
        gradeClerkLimit.set(gradeClerkLimit(request.gradeClerkLimit(), request.gradeClerkLimitFrom()));
    }

    private static RoundingSetting rounding(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new RoundingSetting(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new RoundingSetting(true, null, from);
        }
        if (!DatedSettings.HALF_UP.equals(value) && !DatedSettings.HALF_EVEN.equals(value)) {
            throw new InvalidSettingException("The rounding mode must be half-up or half-even, or blank.");
        }
        return new RoundingSetting(true, value, from);
    }

    private static AlertThreshold alertThreshold(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new AlertThreshold(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new AlertThreshold(true, null);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The alert threshold must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new AlertThreshold(true, new BigDecimal(value).setScale(2));
    }

    private static FiscalStartMonth fiscalStartMonth(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new FiscalStartMonth(false, SettingRepository.DEFAULT_FISCAL_START_MONTH);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new FiscalStartMonth(true, SettingRepository.DEFAULT_FISCAL_START_MONTH);
        }
        int month = value.matches("\\d{1,2}") ? Integer.parseInt(value) : 0;
        if (month < 1 || month > 12) {
            throw new InvalidSettingException("The fiscal start month must be a month from 1 to 12, or blank.");
        }
        return new FiscalStartMonth(true, month);
    }

    private static Parent parent(String requested) {
        if (requested == null) {
            return new Parent(false, null);
        }
        String value = requested.trim();
        return new Parent(true, value.isEmpty() ? null : value);
    }

    private static Pot pot(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new Pot(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Pot(true, null);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The pot must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new Pot(true, new BigDecimal(value).setScale(2));
    }

    /** A from-date typed with a setting (null: left out or blank, the sheet's today). */
    private static LocalDate from(String requested, String what) throws InvalidSettingException {
        String value = requested == null ? "" : requested.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("The " + what + " from-date must be a date (yyyy-MM-dd), or blank.");
        }
    }

    private static PermissionChanges permissions(Map<String, String> requested) throws InvalidSettingException {
        Map<String, String> entries = new LinkedHashMap<>();
        if (requested == null) {
            return new PermissionChanges(entries);
        }
        for (String action : requested.keySet()) {
            if (!Permissions.ACTIONS.contains(action)) {
                throw new InvalidSettingException("Permissions can only be set for open, settle, void, reassign, close or reopen.");
            }
        }
        for (String action : Permissions.ACTIONS) {
            if (!requested.containsKey(action)) {
                continue;
            }
            String value = requested.get(action) == null ? "" : requested.get(action).trim();
            if (value.isEmpty()) {
                entries.put(action, "");
            } else if (Permissions.parse(value) == null) {
                throw new InvalidSettingException("The roles for " + action + " must be clerk or steward, comma separated, or blank.");
            } else {
                entries.put(action, Permissions.label(value));
            }
        }
        return new PermissionChanges(entries);
    }

    private static DefaultGrade defaultGrade(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new DefaultGrade(false, null);
        }
        String value = requested.trim().toUpperCase();
        if (value.isEmpty()) {
            return new DefaultGrade(true, null);
        }
        if (!GRADES.contains(value)) {
            throw new InvalidSettingException("The default grade must be A, B or C, or blank.");
        }
        return new DefaultGrade(true, value);
    }

    private static ProofKinds proofKinds(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new ProofKinds(false, List.of());
        }
        List<String> kinds = new ArrayList<>();
        for (String part : requested.split(",")) {
            String kind = part.trim();
            if (kind.isEmpty()) {
                continue;
            }
            if (!ProofKind.isKind(kind)) {
                throw new InvalidSettingException("The proof kinds must be receipt or photo, comma separated, or blank.");
            }
            if (!kinds.contains(kind)) {
                kinds.add(kind);
            }
        }
        return new ProofKinds(true, kinds);
    }

    private static ReviewSla reviewSla(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new ReviewSla(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ReviewSla(true, null);
        }
        if (!DAYS.matcher(value).matches()) {
            throw new InvalidSettingException("The review SLA must be a whole number of days, 0 or more, or blank.");
        }
        return new ReviewSla(true, Integer.parseInt(value));
    }

    private static EscalateAfter escalateAfter(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new EscalateAfter(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new EscalateAfter(true, null);
        }
        if (!DAYS.matcher(value).matches()) {
            throw new InvalidSettingException("The escalation gap must be a whole number of days, 0 or more, or blank.");
        }
        return new EscalateAfter(true, Integer.parseInt(value));
    }

    private static GraceDays graceDays(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new GraceDays(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new GraceDays(true, null);
        }
        if (!DAYS.matcher(value).matches()) {
            throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank.");
        }
        return new GraceDays(true, Integer.parseInt(value));
    }

    private static GradeGrace gradeGrace(Map<String, String> requested, Map<String, String> requestedFrom)
            throws InvalidSettingException {
        Map<String, Integer> grace = new LinkedHashMap<>();
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (requested == null) {
            return new GradeGrace(grace, from);
        }
        for (String grade : requested.keySet().stream().sorted().toList()) {
            if (!GRADES.contains(grade)) {
                throw new InvalidSettingException("Grace days can only be set for grade A, B or C.");
            }
            String value = requested.get(grade) == null ? "" : requested.get(grade).trim();
            if (value.isEmpty()) {
                grace.put(grade, null);
            } else if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The grace days for a grade must be a whole number of days, 0 or more, or blank.");
            } else {
                grace.put(grade, Integer.parseInt(value));
            }
            LocalDate gradeFrom = from(requestedFrom == null ? null : requestedFrom.get(grade), "grade " + grade + " grace");
            if (gradeFrom != null) {
                from.put(grade, gradeFrom);
            }
        }
        return new GradeGrace(grace, from);
    }

    private static GradeClerkLimit gradeClerkLimit(Map<String, String> requested, Map<String, String> requestedFrom)
            throws InvalidSettingException {
        Map<String, BigDecimal> limits = new LinkedHashMap<>();
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (requested == null) {
            return new GradeClerkLimit(limits, from);
        }
        for (String grade : requested.keySet().stream().sorted().toList()) {
            if (!GRADES.contains(grade)) {
                throw new InvalidSettingException("A clerk limit can only be set for grade A, B or C.");
            }
            String value = requested.get(grade) == null ? "" : requested.get(grade).trim();
            if (value.isEmpty()) {
                limits.put(grade, null);
            } else if (!AMOUNT.matcher(value).matches()) {
                throw new InvalidSettingException("The clerk limit for a grade must be 0.00 or more, with at most two decimals, or blank.");
            } else {
                limits.put(grade, new BigDecimal(value).setScale(2));
            }
            LocalDate gradeFrom = from(requestedFrom == null ? null : requestedFrom.get(grade), "grade " + grade + " clerk limit");
            if (gradeFrom != null) {
                from.put(grade, gradeFrom);
            }
        }
        return new GradeClerkLimit(limits, from);
    }

    private static GradeProofThreshold gradeProofThreshold(Map<String, String> requested, Map<String, String> requestedFrom)
            throws InvalidSettingException {
        Map<String, BigDecimal> thresholds = new LinkedHashMap<>();
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (requested == null) {
            return new GradeProofThreshold(thresholds, from);
        }
        for (String grade : requested.keySet().stream().sorted().toList()) {
            if (!GRADES.contains(grade)) {
                throw new InvalidSettingException("A proof threshold can only be set for grade A, B or C.");
            }
            String value = requested.get(grade) == null ? "" : requested.get(grade).trim();
            if (value.isEmpty()) {
                thresholds.put(grade, null);
            } else if (!AMOUNT.matcher(value).matches()) {
                throw new InvalidSettingException("The proof threshold for a grade must be 0.00 or more, with at most two decimals, or blank.");
            } else {
                thresholds.put(grade, new BigDecimal(value).setScale(2));
            }
            LocalDate gradeFrom = from(requestedFrom == null ? null : requestedFrom.get(grade), "grade " + grade + " proof threshold");
            if (gradeFrom != null) {
                from.put(grade, gradeFrom);
            }
        }
        return new GradeProofThreshold(thresholds, from);
    }

    private static Cap cap(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new Cap(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Cap(true, null, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The cap must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new Cap(true, new BigDecimal(value).setScale(2), from);
    }

    private static ProofThreshold proofThreshold(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new ProofThreshold(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ProofThreshold(true, null, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The proof threshold must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new ProofThreshold(true, new BigDecimal(value).setScale(2), from);
    }

    private static NoteThreshold noteThreshold(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new NoteThreshold(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new NoteThreshold(true, null, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The note threshold must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new NoteThreshold(true, new BigDecimal(value).setScale(2), from);
    }

    private static ReviewThreshold reviewThreshold(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new ReviewThreshold(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ReviewThreshold(true, null, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The review threshold must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new ReviewThreshold(true, new BigDecimal(value).setScale(2), from);
    }

    private static ClerkLimit clerkLimit(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new ClerkLimit(false, null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ClerkLimit(true, null, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new ClerkLimit(true, new BigDecimal(value).setScale(2), from);
    }

    private static Terms terms(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new Terms(null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Terms(SettingRepository.DEFAULT_TERMS, from);
        }
        if (!DAYS.matcher(value).matches()) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        return new Terms(Integer.parseInt(value), from);
    }

    private static GradeTerms gradeTerms(Map<String, String> requested, Map<String, String> requestedFrom)
            throws InvalidSettingException {
        Map<String, Integer> terms = new LinkedHashMap<>();
        Map<String, LocalDate> from = new LinkedHashMap<>();
        if (requested == null) {
            return new GradeTerms(terms, from);
        }
        for (String grade : requested.keySet().stream().sorted().toList()) {
            if (!GRADES.contains(grade)) {
                throw new InvalidSettingException("Terms can only be set for grade A, B or C.");
            }
            String value = requested.get(grade) == null ? "" : requested.get(grade).trim();
            if (value.isEmpty()) {
                terms.put(grade, null);
            } else if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms for a grade must be a whole number of days, 0 or more, or blank.");
            } else {
                terms.put(grade, Integer.parseInt(value));
            }
            LocalDate gradeFrom = from(requestedFrom == null ? null : requestedFrom.get(grade), "grade " + grade + " terms");
            if (gradeFrom != null) {
                from.put(grade, gradeFrom);
            }
        }
        return new GradeTerms(terms, from);
    }

    private static LatePct latePct(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new LatePct(null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new LatePct(SettingRepository.DEFAULT_LATE_PCT, from);
        }
        if (!PERCENT.matcher(value).matches()) {
            throw new InvalidSettingException("The late percent must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new LatePct(new BigDecimal(value).setScale(2), from);
    }

    private static PolicyDate policyDate(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new PolicyDate(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new PolicyDate(true, null);
        }
        try {
            return new PolicyDate(true, LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("The policy date must be a date (yyyy-MM-dd), or blank.");
        }
    }

    private static EntryDate entryDate(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new EntryDate(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new EntryDate(true, null);
        }
        try {
            return new EntryDate(true, LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("The entry date must be a date (yyyy-MM-dd), or blank.");
        }
    }

    private static PeriodNumberingFrom periodNumberingFrom(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new PeriodNumberingFrom(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new PeriodNumberingFrom(true, null);
        }
        try {
            return new PeriodNumberingFrom(true, LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("The period numbering from date must be a date (yyyy-MM-dd), or blank.");
        }
    }

    private static Floor floor(String requested, LocalDate from) throws InvalidSettingException {
        if (requested == null) {
            return new Floor(null, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Floor(SettingRepository.DEFAULT_FLOOR, from);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The floor must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new Floor(new BigDecimal(value).setScale(2), from);
    }
}
