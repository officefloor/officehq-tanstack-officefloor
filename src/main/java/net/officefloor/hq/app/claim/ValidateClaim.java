package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}|.{1,280}-\\d{1,18}");

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // The amount is required; a blank date takes the sheet's today (filled in when stored); the note may be empty.
    // A negative amount is a correction and must name the number it corrects; no other claim may name one.
    // Whether that number is a SETTLED claim of the same person is checked once the person is loaded.
    // The external reference is kept without surrounding spaces; whether the sheet already uses it is checked once loaded.
    public void validate(@RequestBody ClaimRequest body, Out<ClaimInput> input) throws InvalidClaimException {
        input.set(parse(body));
    }

    /** The claim form's rules on a typed body; shared with the paste box, which checks every line by them. */
    static ClaimInput parse(ClaimRequest body) throws InvalidClaimException {
        String amount = body == null || body.amount() == null ? "" : body.amount().trim();
        String date = body == null || body.date() == null ? "" : body.date().trim();
        String note = body == null || body.note() == null ? "" : body.note().trim();
        String corrects = body == null || body.corrects() == null ? "" : body.corrects().trim();
        String ref = body == null || body.ref() == null ? "" : body.ref().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() < 0 && corrects.isEmpty()) {
            throw new InvalidClaimException("A negative amount is a correction: name the number of the settled claim it corrects.");
        }
        if (value.signum() >= 0 && !corrects.isEmpty()) {
            throw new InvalidClaimException("Only a claim with a negative amount corrects another.");
        }
        if (!corrects.isEmpty() && !NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("Not a claim number: " + corrects);
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date (yyyy-mm-dd): " + date);
            }
        }
        return new ClaimInput(value, parsed, note, corrects.isEmpty() ? null : corrects,
                ref.isEmpty() ? null : ref, tags(body == null ? null : body.tags()));
    }

    /** Comma separated tags as a claim keeps them: each trimmed and lower-cased, blanks and repeats dropped, sorted. */
    static List<String> tags(String typed) {
        if (typed == null) {
            return List.of();
        }
        return Arrays.stream(typed.split(",")).map(t -> t.trim().toLowerCase(Locale.ROOT)).filter(t -> !t.isEmpty())
                .distinct().sorted().toList();
    }
}
