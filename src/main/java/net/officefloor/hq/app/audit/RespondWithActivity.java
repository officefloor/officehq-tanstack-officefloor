package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Responds with the sheet's activity: for every user and every event in the sheet's audit lines, one line
 * "<user> <EVENT> <count>", sorted by user then event. A line's event is its first word, its user its by= field. It
 * reads the file; it records nothing.
 */
public class RespondWithActivity {

    public void respond(@PathVariable("sheet") String sheet, Audit audit, ObjectResponse<List<String>> response) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.lines(sheet)) {
            String[] parts = line.split(" ", 4);
            if (parts.length < 3 || !parts[2].startsWith("by=")) {
                continue;
            }
            String user = parts[2].substring("by=".length());
            counts.computeIfAbsent(user, u -> new TreeMap<>()).merge(parts[0], 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> lines.add(user + " " + event + " " + n)));
        response.send(lines);
    }
}
