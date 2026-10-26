package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A setting's fields: a known key, its value, and for a dated setting an optional from-date (yyyy-mm-dd). The terms are a whole number of days. A grade's terms (terms-<grade>) are a whole number of days, or blank for the sheet's terms. A grade's grace (grace-<grade>) is a whole number of days, or blank for none. The floor is an amount with up to two decimals. The clerk limit is an amount with up to two decimals, or blank for no limit. The late percent is a number with up to two decimals. The rounding mode is half-up or half-even, or blank for none. The policy date is a date (yyyy-mm-dd), or blank for the sheet's today. Period numbering from is a date, or blank for off. The entry date is a date, or blank for off. The grace days are a whole number of days, or blank for off. The cap is an amount with up to two decimals, or blank for none. The proof threshold is an amount with up to two decimals, or blank for none. A grade's proof threshold (proof-threshold-<grade>) is an amount with up to two decimals, or blank for the sheet's. The note threshold is an amount with up to two decimals, or blank for none. The pot is an amount with up to two decimals, or blank for none. The alert threshold is an amount with up to two decimals, or blank for off. The parent is a sheet id, or blank for none. A permission (perm-<action>) is a comma separated list of roles, or blank for the standing rule. */
public class ValidateSetting {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");
    private static final Pattern MONTH = Pattern.compile("\\d{1,2}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> setting, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String text = request == null || request.value() == null ? "" : request.value().trim();
        String fromText = request == null || request.from() == null ? "" : request.from().trim();
        java.time.LocalDate from = null;
        if (!fromText.isEmpty()) {
            if (!Setting.isDated(key)) {
                throw new BadRequestException("The " + key + " setting is not dated; leave its from-date blank");
            }
            try {
                from = java.time.LocalDate.parse(fromText);
            } catch (java.time.format.DateTimeParseException e) {
                throw new BadRequestException("A from-date is a date (yyyy-mm-dd), or blank for the sheet's today");
            }
        }
        if (Setting.TERMS.equals(key)) {
            if (!DAYS.matcher(text).matches()) {
                throw new BadRequestException("Terms are a whole number of days");
            }
            setting.set(new SettingInput(key, String.valueOf(Integer.parseInt(text)), from));
            changedBy.set(by);
            return;
        }
        if (key.startsWith(Setting.TERMS_OF_GRADE) && Setting.GRADES.contains(key.substring(Setting.TERMS_OF_GRADE.length()))) {
            if (!text.isEmpty() && !DAYS.matcher(text).matches()) {
                throw new BadRequestException("A grade's terms are a whole number of days, or blank for the sheet's terms");
            }
            setting.set(new SettingInput(key, text.isEmpty() ? null : String.valueOf(Integer.parseInt(text)), from));
            changedBy.set(by);
            return;
        }
        if (key.startsWith(Setting.GRACE_OF_GRADE) && Setting.GRADES.contains(key.substring(Setting.GRACE_OF_GRADE.length()))) {
            if (!text.isEmpty() && !DAYS.matcher(text).matches()) {
                throw new BadRequestException("A grade's grace is a whole number of days, or blank for none");
            }
            setting.set(new SettingInput(key, text.isEmpty() ? null : String.valueOf(Integer.parseInt(text)), from));
            changedBy.set(by);
            return;
        }
        if (Setting.FLOOR.equals(key)) {
            if (!AMOUNT.matcher(text).matches()) {
                throw new BadRequestException("A floor is an amount with up to two decimals");
            }
            setting.set(new SettingInput(key, new BigDecimal(text).setScale(2).toPlainString(), from));
            changedBy.set(by);
            return;
        }
        if (Setting.LATE_PCT.equals(key)) {
            if (!AMOUNT.matcher(text).matches()) {
                throw new BadRequestException("A late percent is a number with up to two decimals");
            }
            setting.set(new SettingInput(key, new BigDecimal(text).setScale(2).toPlainString(), from));
            changedBy.set(by);
            return;
        }
        if (Setting.ROUNDING.equals(key)) {
            if (!text.isEmpty() && !Setting.ROUNDING_HALF_UP.equals(text) && !Setting.ROUNDING_HALF_EVEN.equals(text)) {
                throw new BadRequestException("A rounding mode is half-up or half-even, or blank for none");
            }
            setting.set(new SettingInput(key, text.isEmpty() ? null : text, from));
            changedBy.set(by);
            return;
        }
        if (Setting.POLICY_DATE.equals(key)) {
            String date = null;
            if (!text.isEmpty()) {
                try {
                    date = java.time.LocalDate.parse(text).toString();
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("A policy date is a date (yyyy-mm-dd), or blank for the sheet's today");
                }
            }
            setting.set(new SettingInput(key, date, from));
            changedBy.set(by);
            return;
        }
        if (Setting.PERIOD_NUMBERING_FROM.equals(key)) {
            String date = null;
            if (!text.isEmpty()) {
                try {
                    date = java.time.LocalDate.parse(text).toString();
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("Period numbering from is a date (yyyy-mm-dd), or blank for off");
                }
            }
            setting.set(new SettingInput(key, date, from));
            changedBy.set(by);
            return;
        }
        if (Setting.ENTRY_DATE.equals(key)) {
            String date = null;
            if (!text.isEmpty()) {
                try {
                    date = java.time.LocalDate.parse(text).toString();
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("An entry date is a date (yyyy-mm-dd), or blank for off");
                }
            }
            setting.set(new SettingInput(key, date, from));
            changedBy.set(by);
            return;
        }
        if (Setting.GRACE_DAYS.equals(key)) {
            if (!text.isEmpty() && !DAYS.matcher(text).matches()) {
                throw new BadRequestException("Grace days are a whole number of days, or blank for off");
            }
            setting.set(new SettingInput(key, text.isEmpty() ? null : String.valueOf(Integer.parseInt(text)), from));
            changedBy.set(by);
            return;
        }
        if (Setting.FISCAL_START_MONTH.equals(key)) {
            if (!MONTH.matcher(text).matches() || Integer.parseInt(text) < 1 || Integer.parseInt(text) > 12) {
                throw new BadRequestException("A fiscal start month is a month from 1 to 12");
            }
            setting.set(new SettingInput(key, String.valueOf(Integer.parseInt(text)), from));
            changedBy.set(by);
            return;
        }
        if (Setting.CAP.equals(key)) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("A cap is an amount with up to two decimals, or blank for none");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (Setting.PROOF_THRESHOLD.equals(key)) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("A proof threshold is an amount with up to two decimals, or blank for none");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (key.startsWith(Setting.PROOF_THRESHOLD_OF_GRADE)
                && Setting.GRADES.contains(key.substring(Setting.PROOF_THRESHOLD_OF_GRADE.length()))) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("A grade's proof threshold is an amount with up to two decimals, or blank for the sheet's");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (Setting.NOTE_THRESHOLD.equals(key)) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("A note threshold is an amount with up to two decimals, or blank for none");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (Setting.ALERT_THRESHOLD.equals(key)) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("An alert threshold is an amount with up to two decimals, or blank for off");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (Setting.POT.equals(key)) {
            String value = null;
            if (!text.isEmpty()) {
                if (!AMOUNT.matcher(text).matches()) {
                    throw new BadRequestException("A pot is an amount with up to two decimals, or blank for none");
                }
                value = new BigDecimal(text).setScale(2).toPlainString();
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (Setting.PARENT.equals(key)) {
            if (text.length() > 200) {
                throw new BadRequestException("A parent is a sheet id, or blank for none");
            }
            setting.set(new SettingInput(key, text.isEmpty() ? null : text, from));
            changedBy.set(by);
            return;
        }
        if (Setting.isPermission(key)) {
            String value = null;
            if (!text.isEmpty()) {
                java.util.List<String> roles = java.util.Arrays.stream(text.split(",", -1)).map(String::trim).toList();
                if (roles.stream().anyMatch(r -> !r.equals(Role.CLERK) && !r.equals(Role.STEWARD))) {
                    throw new BadRequestException("A permission is a comma separated list of roles (clerk, steward), or blank for the standing rule");
                }
                value = String.join(",", roles);
            }
            setting.set(new SettingInput(key, value, from));
            changedBy.set(by);
            return;
        }
        if (!Setting.CLERK_LIMIT.equals(key)) {
            throw new BadRequestException("No such setting: " + key);
        }
        String value = null;
        if (!text.isEmpty()) {
            if (!AMOUNT.matcher(text).matches()) {
                throw new BadRequestException("A clerk limit is an amount with up to two decimals, or blank for no limit");
            }
            value = new BigDecimal(text).setScale(2).toPlainString();
        }
        setting.set(new SettingInput(key, value, from));
        changedBy.set(by);
    }
}
