package net.officefloor.hq.app.auditview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines exactly as the file holds them, newest first. With a name, only the lines carrying
 *  name=&lt;the name&gt; or person=&lt;the name&gt;, compared as names are (case and surrounding spaces ignored). */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        String wanted = name == null ? "" : name.trim();
        List<String> lines = new ArrayList<>(audit.linesOf(sheetId.trim()).stream()
                .filter(line -> wanted.isEmpty() || about(line, "name", wanted) || about(line, "person", wanted))
                .toList());
        Collections.reverse(lines);
        response.send(lines);
    }

    /** Whether the line carries key=&lt;value&gt; as a whole field, the value compared ignoring case. */
    private static boolean about(String line, String key, String value) {
        String prefix = " " + key + "=";
        for (int at = line.indexOf(prefix); at >= 0; at = line.indexOf(prefix, at + 1)) {
            int start = at + prefix.length();
            int end = start + value.length();
            if (line.regionMatches(true, start, value, 0, value.length())
                    && (end == line.length() || line.charAt(end) == ' ')) {
                return true;
            }
        }
        return false;
    }
}
