package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Creates the other sheet when it was never loaded, then takes its figures before the transfer. */
public class SnapshotTransferTarget {
    public void snapshot(@Val Transfer transfer, SheetRepository sheets, RollupService rollups,
            Out<TransferTargetBefore> before) {
        sheets.ensure(transfer.to());
        before.set(new TransferTargetBefore(rollups.compute(transfer.to())));
    }
}
