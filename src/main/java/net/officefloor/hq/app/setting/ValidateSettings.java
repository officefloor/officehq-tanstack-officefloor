package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    private static final Pattern FIGURE = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

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
        body.set(new SettingsInput(limitChanged, limit, floor, terms, latePct, policyChanged, policyDate,
                numberingChanged, numberingFrom, gradeTerms, capChanged, cap));
    }
}
