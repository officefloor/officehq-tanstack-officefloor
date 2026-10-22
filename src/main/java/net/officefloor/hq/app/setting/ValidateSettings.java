package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a clerk limit (when given)
 *  that is blank (no limit) or an amount 0.00 or more with up to two decimals, and a floor (when given) that is an
 *  amount 0.00 or more with up to two decimals, and terms (when given) that
 *  are a whole number of days, and a late percent (when given) that is a percent 0.00 or more with up to two decimals, and a policy date (when given) that is
 *  blank (the sheet's today) or a date yyyy-mm-dd, and a period numbering from (when given) that is blank (off) or a date yyyy-mm-dd, and grades' own terms (when given)
 *  only for grades A, B or C, each blank (none: the sheet's terms) or a whole number of days. */
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
        body.set(new SettingsInput(request.user().trim(), limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : new BigDecimal(floor).setScale(2),
                terms == null ? null : Integer.valueOf(terms),
                latePct == null ? null : new BigDecimal(latePct).setScale(2),
                policy != null, policyDate, numbering != null, numberingFrom, gradeTerms));
    }
}
