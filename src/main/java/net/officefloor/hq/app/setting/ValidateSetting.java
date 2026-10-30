package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

public class ValidateSetting {

    public static final String CLERK_LIMIT = "clerk-limit";

    public static final String FLOOR = "floor";

    public static final String CAP = "cap";

    /** Above it (in force on a claim's date) a claim needs a proof to be settled or approved; blank: none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** A grade's proof threshold: proof-threshold-<grade>; dated; blank: none (the sheet's applies). */
    public static final String GRADE_PROOF_THRESHOLD = "proof-threshold-";

    /** Above it (in force on a claim's date) a claim cannot be opened with an empty note; blank: none. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    public static final String TERMS = "terms";

    /** A grade's own terms: terms-<grade>. */
    public static final String GRADE_TERMS = "terms-";

    /** A grade's grace days: grace-<grade>; dated; blank: none. */
    public static final String GRADE_GRACE = "grace-";

    static final List<String> GRADES = List.of("A", "B", "C");

    public static final String LATE_PCT = "late-pct";

    public static final String POLICY_DATE = "policy-date";

    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** Not dated: while set, the default date of a new claim in place of the sheet's today; blank: off. */
    public static final String ENTRY_DATE = "entry-date";

    /** Not dated: when the sheet's today is changed, an open period whose to-date plus them is before it is closed; blank: off. */
    public static final String GRACE_DAYS = "grace-days";

    /** An action's permission: perm-<action>, the roles that may take it. */
    public static final String PERM = "perm-";

    public static final List<String> ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** A number of days: 0 or more, whole. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    /** A percent: 0 or more, with at most two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    /** An amount: 0 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingRequest request, Out<ValidSetting> body, Settings settings)
            throws InvalidSettingException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String key = request.key() == null ? "" : request.key().trim();
        if (!CLERK_LIMIT.equals(key) && !FLOOR.equals(key) && !CAP.equals(key) && !PROOF_THRESHOLD.equals(key) && !NOTE_THRESHOLD.equals(key) && !TERMS.equals(key) && !LATE_PCT.equals(key)
                && !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !ENTRY_DATE.equals(key) && !GRACE_DAYS.equals(key)
                && !(key.startsWith(PERM) && ACTIONS.contains(key.substring(PERM.length())))
                && !(key.startsWith(GRADE_TERMS) && GRADES.contains(key.substring(GRADE_TERMS.length())))
                && !(key.startsWith(GRADE_GRACE) && GRADES.contains(key.substring(GRADE_GRACE.length())))
                && !(key.startsWith(GRADE_PROOF_THRESHOLD) && GRADES.contains(key.substring(GRADE_PROOF_THRESHOLD.length())))) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        String raw = request.value() == null ? "" : request.value().trim();
        if (key.startsWith(PERM)) {
            // Not dated: a comma separated list of roles; blank for the standing rule.
            List<String> roles = new java.util.ArrayList<>();
            for (String part : raw.isEmpty() ? new String[0] : raw.split(",")) {
                String role = part.trim();
                if (!Settings.CLERK.equals(role) && !Settings.STEWARD.equals(role)) {
                    throw new InvalidSettingException("A permission must be a comma separated list of the roles clerk and steward, or blank for the standing rule");
                }
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
            body.set(new ValidSetting(key, null, request.user(), null, null, String.join(",", roles)));
            return;
        }
        if (GRACE_DAYS.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The grace days must be a whole number of days, 0 or more, or blank for off");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, null));
            return;
        }
        // A dated setting's from-date: blank for the sheet's today.
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!rawFrom.isEmpty() && !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !ENTRY_DATE.equals(key)) {
            try {
                from = LocalDate.parse(rawFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The from-date must be a date (YYYY-MM-DD), or blank for the sheet's today");
            }
        }
        if (POLICY_DATE.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: the sheet's today
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The policy date must be a date (YYYY-MM-DD), or blank for the sheet's today");
            }
            return;
        }
        if (PERIOD_NUMBERING_FROM.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The period numbering from must be a date (YYYY-MM-DD), or blank for off");
            }
            return;
        }
        if (ENTRY_DATE.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, null)); // blank: off
                return;
            }
            try {
                body.set(new ValidSetting(key, null, request.user(), LocalDate.parse(raw), null));
            } catch (DateTimeParseException e) {
                throw new InvalidSettingException("The entry date must be a date (YYYY-MM-DD), or blank for off");
            }
            return;
        }
        if (TERMS.equals(key) || key.startsWith(GRADE_TERMS)) {
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, from));
            return;
        }
        if (!GRACE_DAYS.equals(key) && key.startsWith(GRADE_GRACE)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!DAYS.matcher(raw).matches()) {
                throw new InvalidSettingException("A grade's grace days must be a whole number of days, 0 or more, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(Integer.parseInt(raw)), request.user(), null, from));
            return;
        }
        if (LATE_PCT.equals(key)) {
            if (!PERCENT.matcher(raw).matches()) {
                throw new InvalidSettingException("The late percent must be a percent of 0.00 or more with at most two decimals");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (CAP.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: no cap
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The cap must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (PROOF_THRESHOLD.equals(key) || key.startsWith(GRADE_PROOF_THRESHOLD)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The proof threshold must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (NOTE_THRESHOLD.equals(key)) {
            if (raw.isEmpty()) {
                body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: none
                return;
            }
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The note threshold must be an amount of 0.00 or more with at most two decimals, or blank for none");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (FLOOR.equals(key)) {
            if (!AMOUNT.matcher(raw).matches()) {
                throw new InvalidSettingException("The floor must be an amount of 0.00 or more with at most two decimals");
            }
            body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
            return;
        }
        if (raw.isEmpty()) {
            body.set(new ValidSetting(key, null, request.user(), null, from)); // blank: no limit
            return;
        }
        if (!AMOUNT.matcher(raw).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more with at most two decimals");
        }
        body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user(), null, from));
    }
}
