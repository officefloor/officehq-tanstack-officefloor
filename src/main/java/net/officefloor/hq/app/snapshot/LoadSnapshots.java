package net.officefloor.hq.app.snapshot;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadSnapshots {
    public void load(@Val Sheet sheet, SnapshotRepository repository, Out<List<Snapshot>> snapshots) {
        snapshots.set(repository.findAll(sheet.id()));
    }
}
