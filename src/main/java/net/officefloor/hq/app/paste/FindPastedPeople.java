package net.officefloor.hq.app.paste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.CheckClaimInputNotRetired;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.CheckClaimInputPeriodOpen;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Finds each line's person by name (as names are compared: ignoring case) and checks its claim against them with the
 * claim form's rules: no claim after the person's retirement date, none due by (its date plus the terms it would get) inside a closed period, no external reference another claim on the sheet has. Any line refused
 * refuses the whole paste, before anything is written. A line naming a date (blank: the sheet's entry date, else its
 * today) and note that match one of its person's claims as they stand before the paste (void and deleted claims never
 * match) updates the newest such DRAFT instead of adding a claim; a line whose matches include no DRAFT is refused.
 */
public class FindPastedPeople {
    public void find(@Val PastedLines pasted, @Val Sheet sheet, PersonRepository people, SheetRepository sheets,
            PeriodRepository periods, SettingRepository settings, HolidayRepository holidays, ClaimRepository claims) throws InvalidClaimException {
        List<PastedLine> found = new ArrayList<>();
        int number = 0;
        for (PastedLine line : pasted.lines()) {
            number++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new InvalidClaimException("Line " + number + ": " + line.name() + " is not on the sheet.");
            }
            try {
                new CheckClaimInputNotRetired().check(line.input(), person, sheet.id(), sheets);
                new CheckClaimInputPeriodOpen().check(line.input(), person, sheet.id(), sheets, settings, holidays,
                        periods);
                if (line.input().ref() != null && claims.refUsed(sheet.id(), line.input().ref(), null)) {
                    throw new InvalidClaimException("The reference " + line.input().ref() + " is already used on the sheet.");
                }
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
            found.add(new PastedLine(line.name(), line.input(), person,
                    draftMatched(line, person, number, sheets, claims)));
        }
        pasted.lines().clear();
        pasted.lines().addAll(found);
    }

    /** The id of the newest DRAFT the line updates, or null when it matches no claim (it adds one). */
    private static Long draftMatched(PastedLine line, Person person, int number, SheetRepository sheets,
            ClaimRepository claims) throws InvalidClaimException {
        LocalDate date = line.input().date() != null ? line.input().date() : sheets.entryDate(person.sheetId());
        String note = line.input().note() == null ? "" : line.input().note();
        List<Claim> matches = claims.findForPerson(person.sheetId(), person.id()).stream()
                .filter((c) -> c.status() != ClaimStatus.VOID && c.status() != ClaimStatus.DELETED
                        && c.date().equals(date) && note.equals(c.note() == null ? "" : c.note()))
                .toList();
        if (matches.isEmpty()) {
            return null;
        }
        // Newest first, so the first DRAFT is the newest.
        return matches.stream().filter((c) -> c.status() == ClaimStatus.DRAFT).map(Claim::id).findFirst()
                .orElseThrow(() -> new InvalidClaimException("Line " + number + ": " + person.name()
                        + "'s claim on " + date + " with that note is no longer a DRAFT."));
    }
}
