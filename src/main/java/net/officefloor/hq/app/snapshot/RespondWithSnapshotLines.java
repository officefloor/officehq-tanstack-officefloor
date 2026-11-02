package net.officefloor.hq.app.snapshot;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every snapshot line on the sheet, oldest first. */
public class RespondWithSnapshotLines {
    public void respond(@PathVariable("sheet") String sheet, SnapshotLines lines,
            ObjectResponse<List<SnapshotLine>> response) {
        response.send(lines.list(sheet));
    }
}
