package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Adds a DRAFT claim dated the sheet's today, with an empty note, for the person of that name or alias (name
 * compared as the person form compares it), first adding the person with
 * the sheet's default grade when no one on the sheet has that name (refused when the name is used on another sheet of its tree). One change: a refused claim leaves no new person behind.
 */
public class SaveQuickClaim {

    public void save(@PathVariable("sheetId") String sheetId, @Val ValidQuickClaim request, People people, Claims claims,
            Sheets sheets, Periods periods, Audit audit, @Val RollupFigures before, Rollup rollup,
            net.officefloor.hq.app.setting.Settings settings, ClaimLog log, net.officefloor.hq.app.holiday.Holidays holidays,
            ObjectResponse<ResponseEntity<ClaimResponse>> response) throws InvalidClaimException {
        LocalDate date = settings.claimDate(sheetId);
        String grade = settings.defaultGrade(sheetId);
        Person person = people.findByName(sheetId, request.name());
        // Refuse before anything is written: the claim's period goes by the due-by it would be fixed with.
        LocalDate dueBy = holidays.dueBy(sheetId, date, person != null
                ? settings.termsOnCreation(sheetId, person)
                : settings.termsFor(sheetId, grade, sheets.today(sheetId)));
        if (periods.isClosed(sheetId, dueBy)) {
            throw new InvalidClaimException("No claim can be added inside a closed period");
        }
        if (person != null && !person.allowsClaimOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date");
        }
        RollupFigures beforeClaim = before;
        if (person == null) {
            if (people.nameTaken(sheetId, request.name(), null)) {
                throw new InvalidClaimException("The name is already in use");
            }
            person = people.add(sheetId, request.name(), grade);
            audit.recordOnCommit("PERSON_ADDED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " grade=" + person.getGrade());
            rollup.recordChanges(sheetId, request.user(), before);
            beforeClaim = rollup.figures(sheetId);
        }
        Claim claim = claims.add(sheetId, person.getId(), request.amount(), date, "", dueBy);
        log.record(claim, person, request.user());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_ADDED", request.user(), person, claim, claim.getNumber()));
        LateEntry.mark(claim, person, request.user(), claims, periods, audit);
        rollup.recordChanges(sheetId, request.user(), beforeClaim);
        response.send(new ResponseEntity<>(ClaimResponse.from(claim), HttpStatus.CREATED));
    }
}
