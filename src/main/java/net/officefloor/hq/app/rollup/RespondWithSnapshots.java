package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every snapshot on the sheet, oldest first; as of a date (?asOf=), only those written while the sheet's today was
 *  on or before it (the day they leave). */
public class RespondWithSnapshots {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SnapshotRepository snapshots,
            ObjectResponse<List<Snapshot>> response) {
        LocalDate end = AsOf.parse(asOf);
        List<Snapshot> all = snapshots.all(sheetId.trim());
        response.send(end == null ? all
                : all.stream().filter(s -> !LocalDate.parse(s.day()).isAfter(end)).toList());
    }
}
