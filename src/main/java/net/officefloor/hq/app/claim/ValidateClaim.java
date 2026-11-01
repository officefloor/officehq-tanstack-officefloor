package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}|\\S{1,200}-\\d{1,9}");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody ClaimRequest request, Settings settings,
            Out<ClaimInput> body) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        String ref = request == null || request.ref() == null ? "" : request.ref().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals");
        }
        // A negative amount is a correction: it must name, by the number it shows (on the sheet, or
        // within its period: <period>-<n>), the claim it corrects; no other claim may.
        BigDecimal value = new BigDecimal(amount);
        if (value.signum() < 0 && !NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("A negative amount must name the number of the claim it corrects");
        }
        if (value.signum() >= 0 && !corrects.isEmpty()) {
            throw new InvalidClaimException("Only a correction (a negative amount) corrects a claim");
        }
        LocalDate on;
        try {
            on = date.isEmpty() ? settings.entryDay(sheet) : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Not a valid date: " + date);
        }
        body.set(new ClaimInput(value, on, note, value.signum() < 0 ? numbered(corrects) : null,
                ref.isEmpty() ? null : ref, ClaimInput.tagsOf(request == null ? null : request.tags())));
    }

    /** The number as a claim shows it: a number on the sheet without leading zeros, a period number as given. */
    private static String numbered(String corrects) {
        return corrects.chars().allMatch(Character::isDigit) ? Integer.toString(Integer.parseInt(corrects)) : corrects;
    }
}
