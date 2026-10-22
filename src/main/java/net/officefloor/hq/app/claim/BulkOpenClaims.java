package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Open each selected claim that can be opened, in list order (people in the order they were added, newest
 *  claim first within a person), giving each the sheet's next number. Each is opened as a single open would be,
 *  with its CLAIM_OPENED line and then how the sheet's figures moved over it. A selected claim that cannot be
 *  opened (not on the sheet, not a DRAFT, or dated in a closed period) is left as it is and counted as refused. */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpenInput input, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, Rollups rollups, Audit audit,
            Out<BulkOpenResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        int opened = 0;
        for (Person person : people.list(sheet)) {
            List<Claim> theirs = claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).toList();
            for (Claim claim : theirs) {
                if (claim.status().canBecome(ClaimStatus.OPEN) && periods.closedOn(sheet, claim.date()) == null) {
                    Rollup before = rollups.take(sheet);
                    String number = claims.nextNumber(sheet, claim.date());
                    claims.changeStatus(claim, ClaimStatus.OPEN, number, claim.settledDue(), input.user());
                    audit.record("CLAIM_OPENED sheet=" + sheet + " by=" + input.user() + " person=" + person.name()
                            + " amount=" + claim.amount().toPlainString() + " date=" + claim.date() + " number=" + number);
                    rollups.record(sheet, input.user(), before);
                    opened++;
                }
            }
        }
        result.set(new BulkOpenResponse(opened, input.claimIds().size() - opened));
    }
}
