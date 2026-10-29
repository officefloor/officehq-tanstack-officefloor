package net.officefloor.hq.app.auditview;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithActivity {
    public void respond(@PathVariable("sheetId") String sheetId, Audit audit, ObjectResponse<List<String>> response) {
        response.send(audit.activity(sheetId.trim()));
    }
}
