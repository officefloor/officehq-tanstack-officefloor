package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Re-date each selected claim to the one new date, in list order (people in the order they were added, newest claim
 *  first within a person, as the sheet was when the re-date began), by exactly the rules of re-dating one claim: each
 *  that goes through gets its CLAIM_REDATED line and then how the sheet's figures moved over it. A selected claim
 *  that cannot be re-dated (the sheet is archived, not on the sheet, the user not a steward, not OPEN or PENDING, a
 *  date after its person's retirement, or due in a closed period) is left as it is and counted as refused. */
public class BulkRedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId, @Val BulkRedateInput input, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, RoleRepository roles, SheetRepository sheets,
            Rollups rollups, Audit audit, Out<BulkRedateResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        int redated = 0;
        if (sheets.archived(sheet)) {
            result.set(new BulkRedateResponse(0, input.claimIds().size()));
            return;
        }
        List<Claim> selected = new ArrayList<>();
        for (Person person : people.list(sheet)) {
            claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).forEach(selected::add);
        }
        RedateInput redate = new RedateInput(input.user(), input.date());
        for (Claim listed : selected) {
            Claim claim = claims.find(sheet, listed.id());
            if (claim == null || periods.closedOn(sheet, claim.dueBy()) != null) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            ClaimResponse saved;
            try {
                saved = ClaimRedate.redate(redate, claim, claims, people, roles);
            } catch (ClaimStatusNotAllowedException | RetiredPersonException refused) {
                continue;
            }
            ClaimRedate.audit(redate, claim, saved, people, audit);
            rollups.record(sheet, input.user(), before);
            redated++;
        }
        result.set(new BulkRedateResponse(redated, input.claimIds().size() - redated));
    }
}
