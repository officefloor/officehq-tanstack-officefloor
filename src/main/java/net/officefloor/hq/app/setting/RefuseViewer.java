package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseViewer {
    // A viewer sees every screen and figure but changes nothing: every write is refused before anything is read, stored or recorded.
    public void refuse(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection, Settings settings)
            throws ViewerRefusedException {
        if (settings.isViewer(sheetId.trim(), Audit.user(connection))) {
            throw new ViewerRefusedException("A viewer can change nothing.");
        }
    }
}
