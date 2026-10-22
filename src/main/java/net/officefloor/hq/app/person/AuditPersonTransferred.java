package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Val;

/** Record the transfer in the audit file: on this sheet, then how its figures moved; on the other sheet (and the merge
 *  into the person there of the same name or alias, when there was one), then how its figures moved. */
public class AuditPersonTransferred {
    public void audit(@Val TransferInput input, @Val Transfer transfer, @Val Rollup before, Audit audit,
            Rollups rollups) {
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.fromSheet() + " by=" + input.user() + " name="
                + transfer.name() + " to=" + transfer.toSheet());
        rollups.record(transfer.fromSheet(), input.user(), before);
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.toSheet() + " by=" + input.user() + " name="
                + transfer.name() + " from=" + transfer.fromSheet());
        if (transfer.mergedInto() != null) {
            audit.record("PERSON_MERGED sheet=" + transfer.toSheet() + " by=" + input.user() + " from="
                    + transfer.name() + " into=" + transfer.mergedInto());
        }
        rollups.record(transfer.toSheet(), input.user(), transfer.toBefore());
    }
}
