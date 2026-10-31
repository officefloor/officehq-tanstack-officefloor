package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

/** CLAIM_SHARED: the claim's line (without its number), then with=<holder> share=<share>; then what the share moved. */
public class AuditClaimShared {
    public void audit(@Val ShareRequest request, @Val Claim claim, @Val Person holder, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_SHARED", request.user(), person, claim, null)
                + " with=" + holder.getName() + " share=" + new BigDecimal(request.share()).setScale(2).toPlainString());
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
