package net.officefloor.hq.app.activity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: for every user and every event in the sheet's audit lines, one line
 *  "&lt;user&gt; &lt;EVENT&gt; &lt;count&gt;", sorted by user then event. */
public class RespondWithActivity {
    public void respond(@PathVariable("sheetId") String sheetId, Audit audit,
            ObjectResponse<List<String>> response) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.linesOf(sheetId.trim())) {
            String[] fields = line.split(" ");
            String event = fields[0];
            String user = null;
            for (String field : fields) {
                if (field.startsWith("by=")) {
                    user = field.substring("by=".length());
                    break;
                }
            }
            if (event.isEmpty() || user == null) {
                continue;
            }
            counts.computeIfAbsent(user, u -> new TreeMap<>()).merge(event, 1, Integer::sum);
        }
        List<String> lines = counts.entrySet().stream()
                .flatMap(byUser -> byUser.getValue().entrySet().stream()
                        .map(byEvent -> byUser.getKey() + " " + byEvent.getKey() + " " + byEvent.getValue()))
                .toList();
        response.send(lines);
    }
}
