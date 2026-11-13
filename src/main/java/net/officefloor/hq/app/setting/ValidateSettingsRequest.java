package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
            Out<PeriodNumberingFrom> periodNumberingFrom, Out<GradeTerms> gradeTerms)
            throws InvalidSettingException {
        limit.set(clerkLimit(request.clerkLimit()));
        floor.set(floor(request.floor()));
        terms.set(terms(request.terms()));
        latePct.set(latePct(request.latePct()));
        policyDate.set(policyDate(request.policyDate()));
        periodNumberingFrom.set(periodNumberingFrom(request.periodNumberingFrom()));
        gradeTerms.set(gradeTerms(request.gradeTerms()));
    }

    private static ClerkLimit clerkLimit(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new ClerkLimit(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ClerkLimit(true, null);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new ClerkLimit(true, new BigDecimal(value).setScale(2));
    }

    private static Terms terms(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new Terms(null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Terms(SettingRepository.DEFAULT_TERMS);
        }
        if (!DAYS.matcher(value).matches()) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        return new Terms(Integer.parseInt(value));
    }

    private static GradeTerms gradeTerms(Map<String, String> requested) throws InvalidSettingException {
        Map<String, Integer> terms = new LinkedHashMap<>();
        if (requested == null) {
            return new GradeTerms(terms);
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
        }
        return new GradeTerms(terms);
    }

    private static LatePct latePct(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new LatePct(null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new LatePct(SettingRepository.DEFAULT_LATE_PCT);
        }
        if (!PERCENT.matcher(value).matches()) {
            throw new InvalidSettingException("The late percent must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new LatePct(new BigDecimal(value).setScale(2));
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

    private static Floor floor(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new Floor(null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Floor(SettingRepository.DEFAULT_FLOOR);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The floor must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new Floor(new BigDecimal(value).setScale(2));
    }
}
