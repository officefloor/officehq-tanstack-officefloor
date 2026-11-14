package net.officefloor.hq.app.snapshot;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSnapshots {
    public void respond(@Val List<Snapshot> snapshots, ObjectResponse<List<SnapshotResponse>> response) {
        response.send(snapshots.stream().map(SnapshotResponse::from).toList());
    }
}
