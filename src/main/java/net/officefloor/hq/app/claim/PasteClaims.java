package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Check every pasted line against the sheet first, as the claim form would for its person: the name must be on
 *  the sheet (compared as names are), the claim not dated (a blank date being the sheet's entry date, else its today) after the person's
 *  retirement date, its reference (if any) not already used on the sheet, nor due by (its date plus the terms it would get) inside a closed period. Any failure refuses the whole paste, naming the line, and nothing
 *  is written. Otherwise each line is added in order as a DRAFT claim, just as the claim form adds one: its
 *  CLAIM_ADDED line and then how the sheet's figures moved over it. */
public class PasteClaims {
    public void paste(@PathVariable("sheetId") String sheetId, @Val PasteInput input, PersonRepository people,
            SheetRepository sheets, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods, ClaimRepository claims,
            ClaimLogRepository log, Rollups rollups, Audit audit, Out<PasteResponse> result)
            throws InvalidClaimException {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        List<Person> owners = new ArrayList<>();
        for (PasteLine line : input.lines()) {
            Person person = people.findByName(sheet, line.name());
            if (person == null) {
                throw new InvalidClaimException("Line " + line.number() + ": " + line.name() + " is not on the sheet.");
            }
            if (line.claim().ref() != null && claims.refUsed(sheet, line.claim().ref(), 0)) {
                throw new InvalidClaimException("Line " + line.number() + ": " + RefuseUsedNewRef.message(line.claim().ref()));
            }
            var date = line.claim().date() != null ? line.claim().date() : sheets.newClaimDate(sheet);
            try {
                RetiredPersonException.refuse(person, date);
            } catch (RetiredPersonException e) {
                throw new InvalidClaimException("Line " + line.number() + ": " + e.getMessage());
            }
            Period closed = periods.closedOn(sheet, DueBy.of(person, date, settings, holidays));
            if (closed != null) {
                throw new InvalidClaimException("Line " + line.number() + ": " + new ClosedPeriodException(closed).getMessage());
            }
            owners.add(person);
        }
        for (int i = 0; i < owners.size(); i++) {
            ClaimInput claim = input.lines().get(i).claim();
            Person person = owners.get(i);
            Rollup before = rollups.take(sheet);
            Claim added = SaveNewClaim.insert(claim, person, sheets, settings, holidays, periods, claims, log);
            AuditClaimAdded.record(claim, person, added, audit);
            rollups.record(sheet, input.user(), before);
        }
        result.set(new PasteResponse(owners.size()));
    }
}
