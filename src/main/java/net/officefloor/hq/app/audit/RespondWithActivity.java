package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: one "<user> <EVENT> <count>" line for every user (the by= field) and every event (the
 *  record's first word) among the sheet's audit lines, sorted by user then event. Reads the file; keeps no copy. */
public class RespondWithActivity {
    public void respond(@PathVariable("sheetId") String sheetId, Audit audit, ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.lines()) {
            String[] fields = line.split(" ");
            String onSheet = null;
            String by = null;
            for (String field : fields) {
                if (onSheet == null && field.startsWith("sheet=")) {
                    onSheet = field.substring("sheet=".length());
                } else if (by == null && field.startsWith("by=")) {
                    by = field.substring("by=".length());
                }
            }
            if (sheet.equals(onSheet) && by != null) {
                counts.computeIfAbsent(by, u -> new TreeMap<>()).merge(fields[0], 1, Integer::sum);
            }
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> lines.add(user + " " + event + " " + n)));
        response.send(lines);
    }
}
