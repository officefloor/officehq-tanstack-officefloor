package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SaveClaimCreated {
    public void save(@Val ValidClaim request, @Val Person person, Claims claims, Sheets sheets, Audit audit, @Val RollupFigures before, Rollup rollup, Periods periods,
            net.officefloor.hq.app.setting.Settings settings, ClaimLog log, net.officefloor.hq.app.holiday.Holidays holidays,
            ObjectResponse<ResponseEntity<ClaimResponse>> response) throws InvalidClaimException {
        String sheetId = person.getSheetId();
        LocalDate date = request.date() != null ? request.date() : settings.claimDate(sheetId);
        LocalDate dueBy = holidays.dueBy(sheetId, date, settings.termsFor(sheetId, person.getTerms(), person.gradeOn(date), date));
        if (periods.isClosed(sheetId, dueBy)) {
            throw new InvalidClaimException("No claim can be added inside a closed period");
        }
        if (!person.allowsClaimOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date");
        }
        ValidateClaim.checkCorrects(request, sheetId, person.getId(), claims);
        Claim claim = claims.add(sheetId, person.getId(), request.amount(), date, request.note(), request.corrects(), dueBy);
        log.record(claim, person, request.user());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_ADDED", request.user(), person, claim, claim.getNumber()));
        LateEntry.mark(claim, person, request.user(), claims, periods, audit);
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(new ResponseEntity<>(ClaimResponse.from(claim), HttpStatus.CREATED));
    }
}
