package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePaste {
    // Every line is checked before any is added: name,amount,date,note — the name a person on the sheet (ignoring
    // case and surrounding spaces), the rest by the claim form's rules (a blank date is the entry date, else the sheet's today, the note
    // may hold commas or be absent). A note ending in " #<reference>" gives the claim that reference (that part is not
    // the note), refused when the sheet or an earlier line already uses it (ignoring case). Blank lines are skipped; a
    // paste with no line is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PasteRequest body, People people,
            Claims claims, Settings settings, Holidays holidays, Periods periods, Out<PastedLines> out)
            throws InvalidClaimException, ClaimInClosedPeriodException, ClaimAfterRetirementException {
        String sheet = sheetId.trim();
        String text = body == null || body.text() == null ? "" : body.text();
        List<PastedLine> lines = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        int n = 0;
        for (String line : text.split("\\r?\\n")) {
            n++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            if (fields.length < 2) {
                throw new InvalidClaimException("Line " + n + ": type name,amount,date,note.");
            }
            String name = fields[0].trim();
            Person person = name.isEmpty() ? null : people.findByName(sheet, name);
            if (person == null) {
                throw new InvalidClaimException("Line " + n + ": no person " + name + " on this sheet.");
            }
            String note = fields.length > 3 ? fields[3].trim() : "";
            String ref = "";
            int hash = note.lastIndexOf(" #");
            if (hash >= 0) {
                ref = note.substring(hash + 2);
                note = note.substring(0, hash);
            }
            ClaimInput input;
            try {
                input = ValidateClaim.parse(new ClaimRequest(fields[1], fields.length > 2 ? fields[2] : "", note, "", ref));
                if (input.ref() != null && !refs.add(input.ref().toLowerCase(Locale.ROOT))) {
                    throw new InvalidClaimException("The reference " + input.ref() + " is given twice in the paste.");
                }
                RefuseUsedRef.refuse(input.ref(), sheet, 0, claims);
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + n + ": " + e.getMessage());
            }
            LocalDate date = input.date() != null ? input.date() : settings.entryDate(sheet);
            LocalDate dueBy = holidays.dueBy(sheet, date, person.termsOn(date, settings));
            Period closed = periods.closedOn(sheet, dueBy);
            if (closed != null) {
                throw new ClaimInClosedPeriodException("Line " + n + ": the period " + closed.name()
                        + " is closed: no claim due by " + dueBy + " can be added.");
            }
            RefuseRetiredInput.check(person, date);
            lines.add(new PastedLine(person, input));
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note.");
        }
        out.set(new PastedLines(lines));
    }
}
