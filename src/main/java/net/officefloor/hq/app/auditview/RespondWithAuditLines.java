package net.officefloor.hq.app.auditview;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines exactly as the file holds them, newest first. With a name, only the lines carrying
 *  name=&lt;the name&gt; or person=&lt;the name&gt;, compared as names are (case and surrounding spaces ignored),
 *  followed by the lines about them from the sheet's descendants (its children, their children, and so on), sheets
 *  by id, each sheet's lines newest first, then, when the person by that name was transferred here, by the lines about them from the sheet they came
 *  from, newest first, each as that sheet's file holds it. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "name", required = false) String name, Audit audit, PersonRepository people,
            SheetRepository sheets,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        String wanted = name == null ? "" : name.trim();
        List<String> lines = linesAbout(audit, sheet, wanted);
        if (!wanted.isEmpty()) {
            for (String descendant : descendantsOf(sheets, sheet)) {
                lines.addAll(linesAbout(audit, descendant, wanted));
            }
            people.findBySheetIdAndNameIgnoreCase(sheet, wanted).stream().map(Person::getOriginSheet)
                    .filter(origin -> origin != null && !origin.equals(sheet)).findFirst()
                    .ifPresent(origin -> lines.addAll(linesAbout(audit, origin, wanted)));
        }
        response.send(lines);
    }

    /** The ids of the sheet's descendants, by id. */
    private static TreeSet<String> descendantsOf(SheetRepository sheets, String sheet) {
        TreeSet<String> found = new TreeSet<>();
        Deque<String> todo = new ArrayDeque<>(List.of(sheet));
        while (!todo.isEmpty()) {
            for (String child : sheets.childrenOf(todo.pop())) {
                if (!child.equals(sheet) && found.add(child)) {
                    todo.add(child);
                }
            }
        }
        return found;
    }

    /** The sheet's lines (about the name, when one is given), newest first. */
    private static List<String> linesAbout(Audit audit, String sheet, String wanted) {
        List<String> lines = new ArrayList<>(audit.linesOf(sheet).stream()
                .filter(line -> wanted.isEmpty() || about(line, "name", wanted) || about(line, "person", wanted))
                .toList());
        Collections.reverse(lines);
        return lines;
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
