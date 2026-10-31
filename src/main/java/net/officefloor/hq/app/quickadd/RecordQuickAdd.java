package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.RecordClaimAdded;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.RecordPersonAdded;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs once the person and claim are saved, so a refused quick-add writes nothing. Audited as the
 *  person form then the claim form would: a new person's line and the figures that moved, then the
 *  claim's line and the figures it moved. */
public class RecordQuickAdd {
    public void record(@Val PersonRequest personRequest, @Val ClaimValues values, @Val Person person,
            @Val Claim claim, @Val QuickAddPerson found, @Val RollupBefore before, @Val Actor actor,
            Rollup rollup, ClaimLog log, Audit audit) {
        String sheetId = before.sheetId();
        var since = before.snapshot();
        if (found.created()) {
            new RecordPersonAdded().record(personRequest, person, audit);
            // The claim is saved already, but a DRAFT claim moves no figures: these are the person's.
            rollup.record(sheetId, since, actor.user());
            since = rollup.snapshot(sheetId);
        }
        new RecordClaimAdded().record(values, person, claim, log, audit);
        rollup.record(sheetId, since, actor.user());
    }
}
