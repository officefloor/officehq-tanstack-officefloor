package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings from the body, refusing a clerk limit or floor that is not empty or 0.00 or more with up to two decimals,
 *  or terms (the sheet's or a grade's) that are not empty or a whole number of days, or terms for a grade other than A, B or C, or a late percent that is not empty or 0.00 or more with up to two
 *  decimals, or a policy date or period numbering date that is not empty or a date (yyyy-mm-dd). */
public class ValidateSettings {

    private static final Pattern TERMS = Pattern.compile("\\d{1,4}");

    private static final Pattern LIMIT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> settingsOut)
            throws InvalidSettingException {
        String limit = request == null || request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (!limit.isEmpty() && !LIMIT.matcher(limit).matches()) {
            throw new InvalidSettingException("The clerk limit must be empty or 0.00 or more, with up to two decimals.");
        }
        String floor = request == null || request.floor() == null ? "" : request.floor().trim();
        if (!floor.isEmpty() && !LIMIT.matcher(floor).matches()) {
            throw new InvalidSettingException("The floor must be 0.00 or more, with up to two decimals.");
        }
        String terms = request == null || request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !TERMS.matcher(terms).matches()) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more.");
        }
        Map<String, String> gradeTerms = new LinkedHashMap<>();
        Map<String, String> given = request == null || request.gradeTerms() == null ? Map.of() : request.gradeTerms();
        for (String grade : given.keySet()) {
            if (!Setting.GRADES.contains(grade)) {
                throw new InvalidSettingException("Terms can be set only for grade A, B or C.");
            }
        }
        for (String grade : Setting.GRADES) {
            String value = given.get(grade) == null ? "" : given.get(grade).trim();
            if (!value.isEmpty() && !TERMS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms of grade " + grade + " must be a whole number of days, 0 or more.");
            }
            if (!value.isEmpty()) {
                gradeTerms.put(grade, String.valueOf(Integer.parseInt(value)));
            }
        }
        String late = request == null || request.latePct() == null ? "" : request.latePct().trim();
        if (!late.isEmpty() && !LIMIT.matcher(late).matches()) {
            throw new InvalidSettingException("The late percent must be 0.00 or more, with up to two decimals.");
        }
        String policy = request == null || request.policyDate() == null ? "" : request.policyDate().trim();
        if (!policy.isEmpty()) {
            try {
                policy = LocalDate.parse(policy).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The policy date must be empty or a date (yyyy-mm-dd).");
            }
        }
        String numbering = request == null || request.periodNumberingFrom() == null ? "" : request.periodNumberingFrom().trim();
        if (!numbering.isEmpty()) {
            try {
                numbering = LocalDate.parse(numbering).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The period numbering date must be empty or a date (yyyy-mm-dd).");
            }
        }
        settingsOut.set(new SettingsInput(limit.isEmpty() ? null : new BigDecimal(limit).setScale(2).toPlainString(),
                floor.isEmpty() ? null : new BigDecimal(floor).setScale(2).toPlainString(),
                terms.isEmpty() ? null : String.valueOf(Integer.parseInt(terms)),
                late.isEmpty() ? null : new BigDecimal(late).setScale(2).toPlainString(),
                policy.isEmpty() ? null : policy, numbering.isEmpty() ? null : numbering, gradeTerms));
    }
}
