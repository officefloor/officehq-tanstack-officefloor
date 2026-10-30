package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Re-dates the selected claims to one date, in list order (people in the order they were added, newest
 * claim first within a person), each by the rules of re-dating one claim (see {@link ApplyRedate}) and
 * audited as a single re-dating would be. A selected claim that cannot be re-dated is left as it is and
 * counted as refused; so is a selected id that is not a claim on the sheet. A missing or invalid date, a
 * viewer or an archived sheet refuses them all.
 */
public class BulkRedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId, @RequestBody BulkRedateRequest request,
            Claims claims, People people, Periods periods, Settings settings, Rollup rollup, Audit audit,
            ObjectResponse<BulkRedateResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        ValidRedate valid = null;
        try {
            settings.refuseViewer(sheetId, request.user());
            valid = new ValidRedate(request.user(), LocalDate.parse(request.date() == null ? "" : request.date().trim()));
        } catch (ViewerRefusedException | DateTimeParseException e) {
            // every selected claim is refused
        }
        List<Claim> all = claims.list(sheetId);
        List<Claim> ordered = new ArrayList<>();
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() == person.getId() && selected.contains(String.valueOf(claim.getId()))) {
                    ordered.add(claim);
                }
            }
        }
        int redated = 0;
        if (valid != null) {
            for (Claim claim : ordered) {
                try {
                    ApplyRedate.refuse(claim, valid, settings, people, periods);
                } catch (ClaimStatusRefusedException e) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                LocalDate old = claim.getDate();
                claim.setDate(valid.date());
                claims.redate(claim);
                Person person = people.find(sheetId, claim.getPersonId());
                audit.recordOnCommit(ClaimAudit.line("CLAIM_REDATED", request.user(), person, claim, claim.getNumber())
                        + " old=" + old + " new=" + claim.getDate());
                rollup.recordChanges(sheetId, request.user(), before);
                redated++;
            }
        }
        response.send(new BulkRedateResponse(redated, selected.size() - redated));
    }
}
