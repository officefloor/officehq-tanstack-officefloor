package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Adds the pasted claims as DRAFTs, in order, each audited as CLAIM_ADDED. Every line is checked
 * against the sheet first (the person is on the sheet, matched as names are compared; the date is not
 * inside a closed period nor after the person's retirement), so one refused line writes nothing.
 */
public class SavePasteClaims {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPasteClaims request, People people, Claims claims,
            Sheets sheets, Periods periods, Audit audit, @Val RollupFigures before, Rollup rollup, Settings settings,
            ClaimLog log, net.officefloor.hq.app.holiday.Holidays holidays, ObjectResponse<PasteClaimsResponse> response) throws InvalidClaimException {
        LocalDate today = settings.claimDate(sheetId);
        List<Person> persons = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        int number = 0;
        for (ValidPasteClaims.Line line : request.lines()) {
            number++;
            Person person = people.findByName(sheetId, line.name());
            if (person == null) {
                throw new InvalidClaimException("Claim " + number + ": no one on the sheet is named " + line.name());
            }
            LocalDate date = line.claim().date() != null ? line.claim().date() : today;
            LocalDate dueBy = holidays.dueBy(sheetId, date, settings.termsFor(sheetId, person.getTerms(), person.gradeOn(date), date));
            if (periods.isClosed(sheetId, dueBy)) {
                throw new InvalidClaimException("Claim " + number + ": no claim can be added inside a closed period");
            }
            if (!person.allowsClaimOn(date)) {
                throw new InvalidClaimException("Claim " + number + ": no claim can be dated after the person's retirement date");
            }
            persons.add(person);
            dates.add(date);
        }
        for (int i = 0; i < request.lines().size(); i++) {
            ValidClaim valid = request.lines().get(i).claim();
            Person person = persons.get(i);
            LocalDate date = dates.get(i);
            Claim claim = claims.add(sheetId, person.getId(), valid.amount(), date, valid.note(),
                    holidays.dueBy(sheetId, date, settings.termsFor(sheetId, person.getTerms(), person.gradeOn(date), date)));
            log.record(claim, person, request.user());
            audit.recordOnCommit(ClaimAudit.line("CLAIM_ADDED", request.user(), person, claim, claim.getNumber()));
            LateEntry.mark(claim, person, request.user(), claims, periods, audit);
        }
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(new PasteClaimsResponse(request.lines().size()));
    }
}
