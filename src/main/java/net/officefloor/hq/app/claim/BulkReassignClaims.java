package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Move each selected claim to the chosen person, in list order (people in the order they were added, newest claim
 *  first within a person, as the sheet was when the move began), by exactly the rules of moving one claim: each
 *  that goes through gets its CLAIM_REASSIGNED line and then how the sheet's figures moved over it. A selected claim
 *  that cannot be moved (the sheet is archived, not on the sheet, not allowed by the user's role or from its status,
 *  a correction, to an unknown person or one retired before its date, or dated in a closed period) is left as it is
 *  and counted as refused. */
public class BulkReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @Val BulkReassignInput input, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, Rollups rollups, Audit audit, Out<BulkReassignResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        int reassigned = 0;
        if (sheets.archived(sheet)) {
            result.set(new BulkReassignResponse(0, input.claimIds().size()));
            return;
        }
        List<Claim> selected = new ArrayList<>();
        for (Person person : people.list(sheet)) {
            claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).forEach(selected::add);
        }
        ReassignInput move = new ReassignInput(input.user(), input.personId());
        for (Claim listed : selected) {
            Claim claim = claims.find(sheet, listed.id());
            if (claim == null || periods.closedOn(sheet, claim.dueBy()) != null) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            ClaimResponse saved;
            try {
                saved = ClaimReassign.move(move, claim, claims, people, roles, settings, true);
            } catch (ClaimStatusNotAllowedException | PersonNotFoundException | RetiredPersonException refused) {
                continue;
            }
            ClaimReassign.audit(move, claim, saved, people, audit);
            rollups.record(sheet, input.user(), before);
            reassigned++;
        }
        result.set(new BulkReassignResponse(reassigned, input.claimIds().size() - reassigned));
    }
}
