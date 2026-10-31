package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    /** An amount: 0.00 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    /** Terms: a whole number of days, 0 or more. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    /** The grades terms can be set for. */
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** A percent: 0.00 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest request,
            Out<SettingsChange> change, Out<Actor> actor) throws InvalidSheetRequestException {
        String limit = request == null || request.clerkLimit() == null ? null : request.clerkLimit().trim();
        if (limit != null && !limit.isEmpty() && !AMOUNT.matcher(limit).matches()) {
            throw new InvalidSheetRequestException("The clerk limit must be an amount with up to two decimals, or blank");
        }
        String floor = request == null || request.floor() == null ? null : request.floor().trim();
        if (floor != null && !floor.isEmpty() && !AMOUNT.matcher(floor).matches()) {
            throw new InvalidSheetRequestException("The floor must be an amount with up to two decimals, or blank");
        }
        String terms = request == null || request.terms() == null ? null : request.terms().trim();
        if (terms != null && !terms.isEmpty() && !DAYS.matcher(terms).matches()) {
            throw new InvalidSheetRequestException("The terms must be a whole number of days, or blank");
        }
        Map<String, Integer> gradeTerms = new TreeMap<>();
        if (request != null && request.gradeTerms() != null) {
            for (Map.Entry<String, String> e : request.gradeTerms().entrySet()) {
                if (!GRADES.contains(e.getKey())) {
                    throw new InvalidSheetRequestException("Terms can be set only for grade A, B or C");
                }
                String days = e.getValue() == null ? "" : e.getValue().trim();
                if (!days.isEmpty() && !DAYS.matcher(days).matches()) {
                    throw new InvalidSheetRequestException("The terms of a grade must be a whole number of days, or blank");
                }
                gradeTerms.put(e.getKey(), days.isEmpty() ? null : Integer.parseInt(days));
            }
        }
        String late = request == null || request.latePct() == null ? null : request.latePct().trim();
        if (late != null && !late.isEmpty() && !PCT.matcher(late).matches()) {
            throw new InvalidSheetRequestException("The late percent must be a percent with up to two decimals, or blank");
        }
        String policy = request == null || request.policyDate() == null ? null : request.policyDate().trim();
        LocalDate policyDate = null;
        if (policy != null && !policy.isEmpty()) {
            try {
                policyDate = LocalDate.parse(policy);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("The policy date must be a date (yyyy-mm-dd), or blank");
            }
        }
        String numbering = request == null || request.periodNumberingFrom() == null ? null : request.periodNumberingFrom().trim();
        LocalDate numberingFrom = null;
        if (numbering != null && !numbering.isEmpty()) {
            try {
                numberingFrom = LocalDate.parse(numbering);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("The period numbering from must be a date (yyyy-mm-dd), or blank");
            }
        }
        String by = request == null ? null : request.user();
        actor.set(new Actor(by));
        change.set(new SettingsChange(sheetId, limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : floor.isEmpty() ? Settings.DEFAULT_FLOOR : new BigDecimal(floor).setScale(2),
                terms == null ? null : terms.isEmpty() ? Settings.DEFAULT_TERMS : Integer.parseInt(terms),
                late == null ? null : late.isEmpty() ? Settings.DEFAULT_LATE_PCT : new BigDecimal(late).setScale(2),
                policy != null, policyDate, numbering != null, numberingFrom, gradeTerms, by));
    }
}
