package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every snapshot written on the sheet, oldest first; read as of a date (see AsOf), as they stood then. */
public class RespondWithSnapshots {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, SnapshotRepository snapshots,
            ObjectResponse<List<Snapshot>> response) {
        java.time.LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? snapshots.list(sheetId.trim()) : snapshots.listAsOf(sheetId.trim(), date));
    }
}
