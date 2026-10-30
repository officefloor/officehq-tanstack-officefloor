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
 * Adds a DRAFT claim dated the sheet's today, with an empty note, for the named person (name
 * compared as the person form compares it), first adding the person with grade A when no one on
 * the sheet has that name. One change: a refused claim leaves no new person behind.
 */
public class SaveQuickClaim {

    private static final String GRADE = "A";

    public void save(@PathVariable("sheetId") String sheetId, @Val ValidQuickClaim request, People people, Claims claims,
            Sheets sheets, Periods periods, Audit audit, @Val RollupFigures before, Rollup rollup,
            net.officefloor.hq.app.setting.Settings settings, ClaimLog log,
            ObjectResponse<ResponseEntity<ClaimResponse>> response) throws InvalidClaimException {
        LocalDate date = sheets.today(sheetId);
        // Refuse before anything is written.
        if (periods.isClosed(sheetId, date)) {
            throw new InvalidClaimException("No claim can be added inside a closed period");
        }
        Person person = people.findByName(sheetId, request.name());
        if (person != null && !person.allowsClaimOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date");
        }
        RollupFigures beforeClaim = before;
        if (person == null) {
            person = people.add(sheetId, request.name(), GRADE);
            audit.recordOnCommit("PERSON_ADDED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " grade=" + person.getGrade());
            rollup.recordChanges(sheetId, request.user(), before);
            beforeClaim = rollup.figures(sheetId);
        }
        Claim claim = claims.add(sheetId, person.getId(), request.amount(), date, "",
                date.plusDays(settings.termsFor(sheetId, person.getTerms(), person.gradeOn(date), date)));
        log.record(claim, person, request.user());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_ADDED", request.user(), person, claim, claim.getNumber()));
        rollup.recordChanges(sheetId, request.user(), beforeClaim);
        response.send(new ResponseEntity<>(ClaimResponse.from(claim), HttpStatus.CREATED));
    }
}
