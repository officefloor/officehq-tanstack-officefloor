package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimSplit {
    public void audit(@Val ValidSplit request, @Val Claim claim, @Val ClaimSplit split, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Claim into = split.into();
        audit.recordOnCommit(ClaimAudit.line("CLAIM_SPLIT", request.user(), person, claim, claim.getNumber())
                + " into=" + (into.getNumber() == null ? "" : into.getNumber()));
        audit.recordOnCommit(ClaimAudit.line("CLAIM_OPENED", request.user(), person, into, into.getNumber()));
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
