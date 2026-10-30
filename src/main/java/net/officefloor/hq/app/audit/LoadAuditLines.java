package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sheet's audit lines as the file holds them, newest first; only a person's when a name is given. With a name,
 * the matching lines of the sheet's descendants follow (each sheet's newest first, sheets by id); when that
 * person was transferred in, the lines about them from the sheet they came from follow, newest first.
 */
public class LoadAuditLines {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit, People people, Sheets sheets,
            ObjectResponse<List<String>> response) {
        List<String> lines = new ArrayList<>(audit.lines(sheetId, name));
        if (name != null && !name.isBlank()) {
            for (String descendant : sheets.descendants(sheetId).stream().sorted().toList()) {
                lines.addAll(audit.lines(descendant, name));
            }
            String origin = people.originOf(sheetId, name);
            if (origin != null) {
                lines.addAll(audit.lines(origin, name));
            }
        }
        response.send(lines);
    }
}
