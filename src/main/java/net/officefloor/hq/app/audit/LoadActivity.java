package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: its audit lines (EVENT sheet=<id> by=<user> ...) counted per user and event, one
 *  line "<user> <EVENT> <count>" each, sorted by user then event. It reads the file; it records nothing. */
public class LoadActivity {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection, Audit audit,
            Out<Activity> activity) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.read(id, null)) {
            String[] parts = line.split(" ");
            if (parts.length < 3 || !parts[2].startsWith("by=")) {
                continue;
            }
            String user = parts[2].substring("by=".length());
            counts.computeIfAbsent(user, u -> new TreeMap<>()).merge(parts[0], 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> lines.add(user + " " + event + " " + n)));
        activity.set(new Activity(lines));
    }
}
