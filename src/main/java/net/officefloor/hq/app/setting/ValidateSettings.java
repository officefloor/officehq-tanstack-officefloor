package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {

    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?");

    // The amounts are kept with two decimals. A blank floor leaves the floor as it is; a blank clerk limit is
    // no limit, unless a floor, terms, late percent or policy date are saved with it, when the clerk limit is left as it is (and null
    // leaves it as it is too). Anything else that is not an amount of 0.00 or more with up to two decimals is
    // refused. Blank terms are left as they are; otherwise they must be a whole number of days, 0 or more.
    // A blank late percent is left as it is; otherwise it must be a percent of 0.00 or more, with up to two decimals.
    // A missing policy date is left as it is; a blank one is none (the sheet's today is); otherwise it must be a yyyy-MM-dd date.
    // A grade's own terms are like the terms: blank is left as it is, otherwise a whole number of days, 0 or more,
    // and only a grade (A, B or C) may have them.
    // A grade's grace days are dated like its terms: blank is none, otherwise a whole number of days, 0 or more, and only for a grade (A, B or C).
    // The period numbering from-date is the same: missing is left as it is, blank is off, otherwise a yyyy-MM-dd date.
    // The cap is missing: left as it is, blank: none, otherwise an amount of 0.00 or more with up to two decimals.
    // The proof threshold is like the cap: missing is left as it is, blank is none, otherwise an amount of 0.00 or more with up to two decimals.
    // A grade's proof threshold is dated like it: blank is none (the sheet's is in force), otherwise an amount like it, and only for a grade (A, B or C).
    // The note threshold is like the cap too.
    // The grace days are missing: left as they are, blank: off, otherwise a whole number of days, 0 or more (not dated).
    // The entry date is like the period numbering from-date: missing is left as it is, blank is off (the sheet's today), otherwise a yyyy-MM-dd date.
    // The pot is like the cap, but not dated: missing is left as it is, blank is none.
    // The parent is a sheet id, not dated: missing is left as it is, blank is none. It may not be the sheet itself, nor a
    // sheet that has this one as an ancestor, and it must be a sheet that has been loaded.
    // An action's permitted roles are missing: left as they are, blank: the standing rule, otherwise clerk or steward, separated by commas.
    // A from-date, given by setting key, is blank (the sheet's today) or a yyyy-MM-dd date, and only a dated setting has one.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest body,
            Sheets sheets, Settings store, Out<SheetSettings> settingsOut) throws InvalidSettingException {
        String floor = body == null || body.floor() == null ? "" : body.floor().trim();
        if (!floor.isEmpty() && !AMOUNT.matcher(floor).matches()) {
            throw new InvalidSettingException("The floor must be an amount of 0.00 or more, with up to two decimals.");
        }
        String typed = body == null || body.clerkLimit() == null ? null : body.clerkLimit().trim();
        if (typed != null && !typed.isEmpty() && !AMOUNT.matcher(typed).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more, with up to two decimals.");
        }
        String terms = body == null || body.terms() == null ? "" : body.terms().trim();
        if (!terms.isEmpty() && !DAYS.matcher(terms).matches()) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more.");
        }
        SortedMap<String, String> gradeTerms = new TreeMap<>();
        if (body != null && body.gradeTerms() != null) {
            for (Map.Entry<String, String> entry : body.gradeTerms().entrySet()) {
                String grade = entry.getKey() == null ? "" : entry.getKey().trim();
                String days = entry.getValue() == null ? "" : entry.getValue().trim();
                if (days.isEmpty()) {
                    continue;
                }
                if (!Settings.GRADES.contains(grade)) {
                    throw new InvalidSettingException("Terms can be set only for grade A, B or C.");
                }
                if (!DAYS.matcher(days).matches()) {
                    throw new InvalidSettingException("The terms of grade " + grade + " must be a whole number of days, 0 or more.");
                }
                gradeTerms.put(grade, Integer.toString(Integer.parseInt(days)));
            }
        }
        SortedMap<String, String> gradeGrace = new TreeMap<>();
        if (body != null && body.gradeGrace() != null) {
            for (Map.Entry<String, String> entry : body.gradeGrace().entrySet()) {
                String grade = entry.getKey() == null ? "" : entry.getKey().trim();
                if (entry.getValue() == null) {
                    continue;
                }
                String days = entry.getValue().trim();
                if (!Settings.GRADES.contains(grade)) {
                    throw new InvalidSettingException("Grace days can be set only for grade A, B or C.");
                }
                if (!days.isEmpty() && !DAYS.matcher(days).matches()) {
                    throw new InvalidSettingException("The grace days of grade " + grade + " must be a whole number of days, 0 or more.");
                }
                gradeGrace.put(grade, days.isEmpty() ? "" : Integer.toString(Integer.parseInt(days)));
            }
        }
        SortedMap<String, String> gradeProof = new TreeMap<>();
        if (body != null && body.gradeProof() != null) {
            for (Map.Entry<String, String> entry : body.gradeProof().entrySet()) {
                String grade = entry.getKey() == null ? "" : entry.getKey().trim();
                if (entry.getValue() == null) {
                    continue;
                }
                String amount = entry.getValue().trim();
                if (!Settings.GRADES.contains(grade)) {
                    throw new InvalidSettingException("A proof threshold can be set only for grade A, B or C.");
                }
                if (!amount.isEmpty() && !AMOUNT.matcher(amount).matches()) {
                    throw new InvalidSettingException("The proof threshold of grade " + grade + " must be an amount of 0.00 or more, with up to two decimals.");
                }
                gradeProof.put(grade, amount.isEmpty() ? "" : twoDecimals(amount));
            }
        }
        String latePct = body == null || body.latePct() == null ? "" : body.latePct().trim();
        if (!latePct.isEmpty() && !AMOUNT.matcher(latePct).matches()) {
            throw new InvalidSettingException("The late percent must be a percent of 0.00 or more, with up to two decimals.");
        }
        String policyDate = body == null || body.policyDate() == null ? null : body.policyDate().trim();
        if (policyDate != null && !policyDate.isEmpty()) {
            try {
                policyDate = LocalDate.parse(policyDate).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The policy date must be a date (yyyy-mm-dd).");
            }
        }
        String numberingFrom = body == null || body.periodNumberingFrom() == null ? null : body.periodNumberingFrom().trim();
        if (numberingFrom != null && !numberingFrom.isEmpty()) {
            try {
                numberingFrom = LocalDate.parse(numberingFrom).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The period numbering from-date must be a date (yyyy-mm-dd).");
            }
        }
        String cap = body == null || body.cap() == null ? null : body.cap().trim();
        if (cap != null && !cap.isEmpty()) {
            if (!AMOUNT.matcher(cap).matches()) {
                throw new InvalidSettingException("The cap must be an amount of 0.00 or more, with up to two decimals.");
            }
            cap = twoDecimals(cap);
        }
        String threshold = body == null || body.proofThreshold() == null ? null : body.proofThreshold().trim();
        if (threshold != null && !threshold.isEmpty()) {
            if (!AMOUNT.matcher(threshold).matches()) {
                throw new InvalidSettingException("The proof threshold must be an amount of 0.00 or more, with up to two decimals.");
            }
            threshold = twoDecimals(threshold);
        }
        String noteThreshold = body == null || body.noteThreshold() == null ? null : body.noteThreshold().trim();
        if (noteThreshold != null && !noteThreshold.isEmpty()) {
            if (!AMOUNT.matcher(noteThreshold).matches()) {
                throw new InvalidSettingException("The note threshold must be an amount of 0.00 or more, with up to two decimals.");
            }
            noteThreshold = twoDecimals(noteThreshold);
        }
        String grace = body == null || body.graceDays() == null ? null : body.graceDays().trim();
        if (grace != null && !grace.isEmpty()) {
            if (!DAYS.matcher(grace).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more.");
            }
            grace = Integer.toString(Integer.parseInt(grace));
        }
        String entryDate = body == null || body.entryDate() == null ? null : body.entryDate().trim();
        if (entryDate != null && !entryDate.isEmpty()) {
            try {
                entryDate = LocalDate.parse(entryDate).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The entry date must be a date (yyyy-mm-dd).");
            }
        }
        String pot = body == null || body.pot() == null ? null : body.pot().trim();
        if (pot != null && !pot.isEmpty()) {
            if (!AMOUNT.matcher(pot).matches()) {
                throw new InvalidSettingException("The pot must be an amount of 0.00 or more, with up to two decimals.");
            }
            pot = twoDecimals(pot);
        }
        String parent = body == null || body.parent() == null ? null : body.parent().trim();
        if (parent != null && !parent.isEmpty()) {
            String sheet = sheetId.trim();
            if (parent.equals(sheet)) {
                throw new InvalidSettingException("A sheet cannot be its own parent.");
            }
            if (!sheets.exists(parent)) {
                throw new InvalidSettingException("The parent must be a sheet that exists.");
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String at = parent; !at.isEmpty() && seen.add(at); at = store.parent(at)) {
                if (at.equals(sheet)) {
                    throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor.");
                }
            }
        }
        Map<String, LocalDate> from = new java.util.HashMap<>();
        if (body != null && body.from() != null) {
            for (Map.Entry<String, String> entry : body.from().entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().trim();
                String date = entry.getValue() == null ? "" : entry.getValue().trim();
                if (date.isEmpty()) {
                    continue;
                }
                if (!Settings.DATED.contains(key)) {
                    throw new InvalidSettingException("Only a setting that carries a figure has a from-date.");
                }
                try {
                    from.put(key, LocalDate.parse(date));
                } catch (DateTimeParseException e) {
                    throw new InvalidSettingException("The from-date must be a date (yyyy-mm-dd).");
                }
            }
        }
        Map<String, String> perms = new java.util.LinkedHashMap<>();
        if (body != null && body.perms() != null) {
            for (String action : body.perms().keySet()) {
                if (!Settings.PERM_ACTIONS.contains(action)) {
                    throw new InvalidSettingException("Permissions are set only for open, settle, void, reassign, close and reopen.");
                }
            }
            for (String action : Settings.PERM_ACTIONS) {
                String typedRoles = body.perms().get(action);
                if (typedRoles == null) {
                    continue;
                }
                java.util.List<String> roles = new java.util.ArrayList<>();
                if (!typedRoles.isBlank()) {
                    for (String role : typedRoles.split(",")) {
                        String r = role.trim();
                        if (!Settings.ROLES.contains(r)) {
                            throw new InvalidSettingException("The roles allowed to " + action + " must be clerk or steward, separated by commas.");
                        }
                        if (!roles.contains(r)) {
                            roles.add(r);
                        }
                    }
                }
                perms.put(action, String.join(",", roles));
            }
        }
        boolean others = !floor.isEmpty() || !terms.isEmpty() || !gradeTerms.isEmpty() || !gradeGrace.isEmpty() || !gradeProof.isEmpty() || !latePct.isEmpty() || policyDate != null
                || numberingFrom != null || cap != null || threshold != null || noteThreshold != null || grace != null || entryDate != null || !perms.isEmpty() || pot != null || parent != null;
        if (typed == null && !others) {
            typed = "";
        }
        String limit = typed == null || (typed.isEmpty() && others) ? null
                : typed.isEmpty() ? "" : twoDecimals(typed);
        settingsOut.set(new SheetSettings(sheetId.trim(), limit, floor.isEmpty() ? null : twoDecimals(floor),
                terms.isEmpty() ? null : Integer.toString(Integer.parseInt(terms)),
                latePct.isEmpty() ? null : twoDecimals(latePct), policyDate, numberingFrom, cap, threshold, grace, noteThreshold, entryDate, gradeTerms, gradeGrace, from, perms, gradeProof, pot, parent));
    }

    private static String twoDecimals(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
