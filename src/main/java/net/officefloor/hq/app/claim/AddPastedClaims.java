package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a DRAFT claim per pasted line, in order, for the person of that name (case and surrounding spaces ignored).
 * Every line is first checked as the claim form checks a new claim (person on the sheet, correction, reference, closed period,
 * retirement); one refusal refuses them all before anything is written. Each added claim is recorded as CLAIM_ADDED,
 * then the rollup lines for what the paste moved.
 */
public class AddPastedClaims {

    public void add(@Val SheetResponse sheet, @Val PasteLines pasted, @Val ChangedBy by, PersonRepository people,
            ClaimRepository claims, SheetRepository sheets, PeriodRepository periods, SettingRepository settings,
            HolidayRepository holidays, Rollup rollup, Audit audit, ClaimLog log, Out<PasteResponse> response) throws BadRequestException {
        LocalDate today = settings.entryDate(sheet.id(), sheets.today(sheet.id()));
        List<Person> persons = new ArrayList<>();
        int n = 0;
        for (PasteLine line : pasted.lines()) {
            n++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new BadRequestException("Line " + n + ": " + line.name() + " is not on this sheet");
            }
            ClaimInput input = line.input();
            LocalDate date = input.date() != null ? input.date() : today;
            try {
                RefuseBadCorrection.refuse(input, sheet.id(), person.getId(), claims);
                RefuseUsedRef.refuse(sheet.id(), input.ref(), -1, claims);
                RefuseClosedDate.refuse(sheet.id(), BuildClaim.dueBy(person, date, settings, holidays), periods);
                RefuseRetiredDate.refuse(person, date);
            } catch (BadRequestException e) {
                throw new BadRequestException("Line " + n + ": " + e.getMessage());
            }
            persons.add(person);
        }

        RollupSnapshot before = rollup.snapshot(sheet.id());
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            ClaimInput input = pasted.lines().get(i).input();
            LocalDate date = input.date() != null ? input.date() : today;
            Claim claim = claims.insert(sheet.id(), person.getId(), input.amount(), date, input.note(),
                    input.corrects(), input.ref(), BuildClaim.terms(person, date, settings));
            if (periods.softClosedOn(sheet.id(), claim.getDueBy())) {
                claims.markLate(claim);
            }
            audit.record(ClaimAudit.added(claim, by, person.getName()));
            if (claim.isLateEntry()) {
                audit.record(ClaimAudit.line("LATE_ENTRY", claim, by, person.getName()));
            }
            log.record(claim, by);
        }
        rollup.record(sheet.id(), before, by);
        response.set(new PasteResponse(persons.size()));
    }
}
