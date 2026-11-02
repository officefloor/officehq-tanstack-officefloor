package net.officefloor.hq.app.paste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line (name,amount,date,note; the note may hold commas, the date may be blank for the sheet's entry
 * date or today, the note may be absent) by the claim form's rules before anything is written, so a refused paste adds
 * nothing. The name must be on the sheet (ignoring case). A paste carries no corrected number, so a negative amount
 * is refused as on the form without one. A note ending in " #<reference>" gives the claim's reference, refused when
 * already used on the sheet or repeated within the paste (ignoring case). Blank lines are skipped; a paste with no line
 * is refused.
 */
public class ValidatePaste {
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody PasteRequest request, People people,
            Settings settings, Holidays holidays, Periods periods, Claims claims, Out<PastedClaims> pasted)
            throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedClaim> lines = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            PastedClaim checked = check(sheet, at, line, people, settings, holidays, periods);
            String ref = checked.input().ref();
            if (ref != null) {
                if (!refs.add(ref.toLowerCase(Locale.ROOT))) {
                    throw new InvalidClaimException("Line " + at + ": the reference " + ref + " is repeated in the paste");
                }
                try {
                    checked.input().checkRef(sheet, null, claims);
                } catch (InvalidClaimException e) {
                    throw new InvalidClaimException("Line " + at + ": " + e.getMessage());
                }
            }
            lines.add(checked);
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note");
        }
        pasted.set(new PastedClaims(lines));
    }

    private static PastedClaim check(String sheet, int at, String line, People people, Settings settings, Holidays holidays,
            Periods periods) throws InvalidClaimException {
        String[] fields = line.split(",", 4);
        String name = fields[0].trim();
        String amount = fields.length > 1 ? fields[1].trim() : "";
        String date = fields.length > 2 ? fields[2].trim() : "";
        String note = fields.length > 3 ? fields[3].trim() : "";
        // A note ending in " #<reference>" gives the claim's reference; that part is not the note.
        String ref = null;
        int hash = note.lastIndexOf(" #");
        if (hash >= 0 && !note.substring(hash + 2).isBlank()) {
            ref = note.substring(hash + 2).trim();
            note = note.substring(0, hash).trim();
        }
        Person person = name.isEmpty() ? null : people.findByName(sheet, name);
        if (person == null) {
            throw new InvalidClaimException("Line " + at + ": no one named " + name + " is on the sheet");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("Line " + at + ": the amount must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(amount);
        if (value.signum() < 0) {
            throw new InvalidClaimException("Line " + at + ": a negative amount must name the claim it corrects");
        }
        LocalDate on;
        try {
            on = date.isEmpty() ? settings.entryDay(sheet) : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Line " + at + ": not a valid date: " + date);
        }
        if (periods.closedOn(sheet, person.dueBy(sheet, on, settings, holidays))) {
            throw new InvalidClaimException("Line " + at + ": a claim may not be due inside a closed period");
        }
        if (person.retiredBefore(on)) {
            throw new InvalidClaimException(
                    "Line " + at + ": a claim may not be dated after the person's retirement date");
        }
        return new PastedClaim(at, person, new ClaimInput(value, on, note, null, ref));
    }
}
