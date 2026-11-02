package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the settings form: the clerk limit is 0.00 or more with up to two decimals, blank meaning no
 *  limit; the floor is 0.00 or more with up to two decimals; the terms are a whole number of days, 0 to
 *  3650; the late percent is 0.00 or more with up to two decimals; the policy date is a date
 *  (yyyy-mm-dd), blank meaning the sheet's today; the period
 *  numbering from-date is a date, blank meaning off; a grade's (A, B or C) terms are like the sheet's, blank
 *  meaning the grade uses the sheet's terms; the cap is 0.00 or more with up to two decimals, blank meaning none; the proof threshold likewise. A from-date given for a
 *  setting with a figure is a date; blank is the sheet's today. A setting left out is not changed. */
public class ValidateSettings {
    private static final Pattern MONEY = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?|\\.\\d{1,2}");
    private static final List<String> GRADES = List.of("A", "B", "C");
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
        for (String grade : GRADES) {
            String value = grades.get(grade) == null ? null : grades.get(grade).trim();
            if (value == null) continue;
            if (!value.isEmpty() && (!TERMS.matcher(value).matches() || Integer.parseInt(value) > 3650)) {
                throw new BadRequestException("terms for grade " + grade + " must be a whole number of days from 0 to 3650");
            }
            gradeTerms.put(grade, value.isEmpty() ? null : Integer.parseInt(value));
        }
        for (String grade : grades.keySet()) {
            if (!GRADES.contains(grade)) throw new BadRequestException("grade must be A, B or C");
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
                default -> key.startsWith("terms-") && gradeTerms.containsKey(key.substring(6));
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
                proof != null, proof == null || proof.isEmpty() ? null : new BigDecimal(proof).setScale(2)));
    }
}
