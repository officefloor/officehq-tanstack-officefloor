package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: one line "<user> <EVENT> <count>" per user and event in its audit lines, by user then event. */
public class LoadActivity {
    public void service(@PathVariable("sheetId") String sheetId, Audit audit, ObjectResponse<List<String>> response) {
        response.send(audit.activity(sheetId));
    }
}
