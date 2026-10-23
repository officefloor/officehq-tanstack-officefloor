package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the file holds them, newest first; with a name, only the lines about that person (a
 *  name= or person= field holding it, case and surrounding spaces ignored), followed by the lines about them on each
 *  of the sheet's descendants (by id, each newest first), then, when the person of that name
 *  (or alias) on this sheet was transferred here, by the lines about them on the sheet they came from, newest first.
 *  Reads the file; keeps no copy. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId, @RequestParam(name = "name", required = false) String name,
            Audit audit, PersonRepository people, SheetRepository sheets, ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> all = audit.lines();
        List<String> lines = about(all, sheet, key);
        if (!key.isEmpty()) {
            for (String descendant : sheets.descendants(sheet)) {
                lines.addAll(about(all, descendant, key));
            }
            Person person = people.findByName(sheet, key);
            if (person != null && person.origin() != null) {
                lines.addAll(about(all, person.origin(), person.name().trim().toLowerCase(Locale.ROOT)));
            }
        }
        response.send(lines);
    }

    /** The sheet's lines (about the person of the key, unless blank), newest first. */
    private static List<String> about(List<String> all, String sheet, String key) {
        List<String> lines = new ArrayList<>();
        for (String line : all) {
            if (onSheet(line, sheet) && (key.isEmpty() || about(line, key))) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    /** Whether the line's sheet= field is the sheet. */
    private static boolean onSheet(String line, String sheet) {
        for (String field : line.split(" ")) {
            if (field.startsWith("sheet=")) {
                return field.substring("sheet=".length()).equals(sheet);
            }
        }
        return false;
    }

    /** Whether a name= or person= field of the line holds the name (already in comparison form). */
    private static boolean about(String line, String key) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String label : new String[] { " name=", " person=" }) {
            int at = lower.indexOf(label);
            while (at >= 0) {
                String rest = lower.substring(at + label.length());
                if (rest.startsWith(key) && (rest.length() == key.length() || rest.charAt(key.length()) == ' ')) {
                    return true;
                }
                at = lower.indexOf(label, at + 1);
            }
        }
        return false;
    }
}
