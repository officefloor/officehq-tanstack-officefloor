package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity: for every user and every event in the sheet's audit lines, one line
 *  <user> <EVENT> <count>, sorted by user then event. Reads the file; records nothing. */
public class RespondWithActivity {

    private static final Pattern EVENT_BY = Pattern.compile("^(\\S+) sheet=\\S+ by=(\\S+)");

    public void respond(@PathVariable("sheetId") String sheetId, Audit audit,
            ObjectResponse<List<String>> response) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();
        for (String line : audit.linesFor(sheetId, null)) {
            Matcher m = EVENT_BY.matcher(line);
            if (m.find()) {
                counts.computeIfAbsent(m.group(2), u -> new TreeMap<>()).merge(m.group(1), 1, Integer::sum);
            }
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> lines.add(user + " " + event + " " + n)));
        response.send(lines);
    }
}
