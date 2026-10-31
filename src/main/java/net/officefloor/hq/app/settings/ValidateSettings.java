package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    /** The settings that are dated, by key. */
    private static final Set<String> DATED = Set.of(Settings.CLERK_LIMIT, Settings.FLOOR, Settings.TERMS,
            Settings.gradeKey("A"), Settings.gradeKey("B"), Settings.gradeKey("C"),
            Settings.graceKey("A"), Settings.graceKey("B"), Settings.graceKey("C"), Settings.LATE_PCT, Settings.CAP,
            Settings.PROOF_THRESHOLD, Settings.proofKey("A"), Settings.proofKey("B"), Settings.proofKey("C"),
            Settings.NOTE_THRESHOLD);

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
        Map<String, Integer> gradeGrace = new TreeMap<>();
        if (request != null && request.gradeGrace() != null) {
            for (Map.Entry<String, String> e : request.gradeGrace().entrySet()) {
                if (!GRADES.contains(e.getKey())) {
                    throw new InvalidSheetRequestException("Grace days can be set only for grade A, B or C");
                }
                String days = e.getValue() == null ? "" : e.getValue().trim();
                if (!days.isEmpty() && !DAYS.matcher(days).matches()) {
                    throw new InvalidSheetRequestException("The grace days of a grade must be a whole number of days, or blank");
                }
                gradeGrace.put(e.getKey(), days.isEmpty() ? null : Integer.parseInt(days));
            }
        }
        Map<String, BigDecimal> gradeProof = new TreeMap<>();
        if (request != null && request.gradeProof() != null) {
            for (Map.Entry<String, String> e : request.gradeProof().entrySet()) {
                if (!GRADES.contains(e.getKey())) {
                    throw new InvalidSheetRequestException("A proof threshold can be set only for grade A, B or C");
                }
                String amount = e.getValue() == null ? "" : e.getValue().trim();
                if (!amount.isEmpty() && !AMOUNT.matcher(amount).matches()) {
                    throw new InvalidSheetRequestException("The proof threshold of a grade must be an amount with up to two decimals, or blank");
                }
                gradeProof.put(e.getKey(), amount.isEmpty() ? null : new BigDecimal(amount).setScale(2));
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
        String entry = request == null || request.entryDate() == null ? null : request.entryDate().trim();
        LocalDate entryDate = null;
        if (entry != null && !entry.isEmpty()) {
            try {
                entryDate = LocalDate.parse(entry);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("The entry date must be a date (yyyy-mm-dd), or blank");
            }
        }
        String cap = request == null || request.cap() == null ? null : request.cap().trim();
        if (cap != null && !cap.isEmpty() && !AMOUNT.matcher(cap).matches()) {
            throw new InvalidSheetRequestException("The cap must be an amount with up to two decimals, or blank");
        }
        String threshold = request == null || request.proofThreshold() == null ? null : request.proofThreshold().trim();
        if (threshold != null && !threshold.isEmpty() && !AMOUNT.matcher(threshold).matches()) {
            throw new InvalidSheetRequestException("The proof threshold must be an amount with up to two decimals, or blank");
        }
        String noteThreshold = request == null || request.noteThreshold() == null ? null : request.noteThreshold().trim();
        if (noteThreshold != null && !noteThreshold.isEmpty() && !AMOUNT.matcher(noteThreshold).matches()) {
            throw new InvalidSheetRequestException("The note threshold must be an amount with up to two decimals, or blank");
        }
        String grace = request == null || request.graceDays() == null ? null : request.graceDays().trim();
        if (grace != null && !grace.isEmpty() && !DAYS.matcher(grace).matches()) {
            throw new InvalidSheetRequestException("The grace days must be a whole number of days, or blank");
        }
        Map<String, String> perms = new LinkedHashMap<>();
        if (request != null && request.perms() != null) {
            for (Map.Entry<String, String> e : request.perms().entrySet()) {
                if (!Settings.PERM_ACTIONS.contains(e.getKey())) {
                    throw new InvalidSheetRequestException("Not an action of the permission matrix: " + e.getKey());
                }
                String value = e.getValue() == null ? "" : e.getValue().trim();
                List<String> roles = new ArrayList<>();
                if (!value.isEmpty()) {
                    for (String r : value.split(",", -1)) {
                        String role = r.trim().toLowerCase(Locale.ROOT);
                        if (!role.equals(Role.CLERK.show()) && !role.equals(Role.STEWARD.show())) {
                            throw new InvalidSheetRequestException("A permission is a comma separated list of roles (clerk, steward), or blank");
                        }
                        if (!roles.contains(role)) roles.add(role);
                    }
                }
                perms.put(e.getKey(), roles.isEmpty() ? null : String.join(",", roles));
            }
        }
        Map<String, LocalDate> froms = new TreeMap<>();
        if (request != null && request.from() != null) {
            for (Map.Entry<String, String> e : request.from().entrySet()) {
                if (!DATED.contains(e.getKey())) {
                    throw new InvalidSheetRequestException("Not a dated setting: " + e.getKey());
                }
                String from = e.getValue() == null ? "" : e.getValue().trim();
                if (from.isEmpty()) continue;
                try {
                    froms.put(e.getKey(), LocalDate.parse(from));
                } catch (DateTimeParseException ex) {
                    throw new InvalidSheetRequestException("A from-date must be a date (yyyy-mm-dd), or blank");
                }
            }
        }
        String by = request == null ? null : request.user();
        actor.set(new Actor(by));
        change.set(new SettingsChange(sheetId, limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : floor.isEmpty() ? Settings.DEFAULT_FLOOR : new BigDecimal(floor).setScale(2),
                terms == null ? null : terms.isEmpty() ? Settings.DEFAULT_TERMS : Integer.parseInt(terms),
                late == null ? null : late.isEmpty() ? Settings.DEFAULT_LATE_PCT : new BigDecimal(late).setScale(2),
                policy != null, policyDate, numbering != null, numberingFrom, gradeTerms, by,
                cap != null, cap == null || cap.isEmpty() ? null : new BigDecimal(cap).setScale(2), froms,
                threshold != null, threshold == null || threshold.isEmpty() ? null : new BigDecimal(threshold).setScale(2), perms,
                grace != null, grace == null || grace.isEmpty() ? null : Integer.parseInt(grace),
                noteThreshold != null, noteThreshold == null || noteThreshold.isEmpty() ? null : new BigDecimal(noteThreshold).setScale(2),
                entry != null, entryDate, gradeGrace, gradeProof));
    }
}
