package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines (see {@link Audit#linesFor}). Filtered by the name of a person who was
 *  transferred here, this sheet's lines about them are followed by the lines about them on the sheet
 *  they came from (by the name they had there), newest first, as that sheet's file holds them.
 *  Filtered by a name, this sheet's lines are followed by the matching lines of each of its
 *  descendants (sheets by id, each newest first), before the lines from the sheet a person came from. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit, People people, Settings settings,
            ObjectResponse<List<String>> response) {
        List<String> lines = new ArrayList<>(audit.linesFor(sheetId, name));
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (!key.isEmpty()) {
            for (String descendant : settings.descendants(sheetId).stream().sorted().toList()) {
                lines.addAll(audit.linesFor(descendant, name));
            }
            Person person = people.named(sheetId, name);
            if (person != null && person.getOriginSheet() != null
                    && person.getName().trim().toLowerCase(Locale.ROOT).equals(key)) {
                lines.addAll(audit.linesFor(person.getOriginSheet(), person.getOriginName()));
            }
        }
        response.send(lines);
    }
}
