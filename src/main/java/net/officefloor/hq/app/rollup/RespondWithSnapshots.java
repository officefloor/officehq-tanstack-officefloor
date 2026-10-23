package net.officefloor.hq.app.rollup;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every snapshot on the sheet, oldest first. */
public class RespondWithSnapshots {
    public void respond(@PathVariable("sheetId") String sheetId, SnapshotRepository snapshots,
            ObjectResponse<List<Snapshot>> response) {
        response.send(snapshots.all(sheetId.trim()));
    }
}
