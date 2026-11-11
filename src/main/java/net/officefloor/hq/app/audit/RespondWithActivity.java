package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's activity: for every user and every event in the sheet's audit lines (read from the
 * audit file), one line {@code <user> <EVENT> <count>}, sorted by user then event. It writes nothing.
 */
public class RespondWithActivity {
    public void respond(@Val Sheet sheet, Audit audit, ObjectResponse<ActivityResponse> response) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String[] head : audit.headsFor(sheet.id())) {
            counts.computeIfAbsent(head[1], u -> new TreeMap<>()).merge(head[0], 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, count) -> lines.add(user + " " + event + " " + count)));
        response.send(new ActivityResponse(lines));
    }
}
