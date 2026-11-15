package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: for every user and every event in the sheet's audit lines, one line
 *  {@code <user> <EVENT> <count>}, sorted by user then event. Reads the file; keeps nothing. */
public class ListActivity {
    public void list(@PathVariable("sheetId") String sheetId, Audit audit,
            ObjectResponse<List<String>> response) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.lines(sheetId.trim(), null)) {
            String[] fields = line.split(" ");
            String by = null;
            for (String field : fields) {
                if (field.startsWith("by=")) {
                    by = field.substring("by=".length());
                    break;
                }
            }
            if (by == null || by.isEmpty() || fields[0].isEmpty()) continue;
            counts.computeIfAbsent(by, u -> new TreeMap<>()).merge(fields[0], 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> lines.add(user + " " + event + " " + n)));
        response.send(lines);
    }
}
