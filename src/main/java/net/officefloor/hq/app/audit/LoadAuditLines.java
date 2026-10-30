package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sheet's audit lines as the file holds them, newest first; only a person's when a name is given. When that
 * person was transferred in, the lines about them from the sheet they came from follow, newest first.
 */
public class LoadAuditLines {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit, People people,
            ObjectResponse<List<String>> response) {
        List<String> lines = new ArrayList<>(audit.lines(sheetId, name));
        if (name != null && !name.isBlank()) {
            String origin = people.originOf(sheetId, name);
            if (origin != null) {
                lines.addAll(audit.lines(origin, name));
            }
        }
        response.send(lines);
    }
}
