package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sheet's audit lines as the file holds them, newest first; only those about the named person when a name is
 * given. With a name, the lines about them from each of the sheet's descendants follow, each sheet's newest first,
 * the sheets by id. When that name is a person transferred here, the lines about them from the sheet they came from follow,
 * newest first, as that sheet's lines are held.
 */
public class RespondWithAuditLines {
    public void respond(@Val Sheet sheet, @RequestParam(value = "name", required = false) String name, Audit audit,
            PersonRepository people, SettingRepository settings, ObjectResponse<List<String>> response) {
        List<String> lines = new ArrayList<>(audit.lines(sheet.id(), name));
        String wanted = name == null ? "" : name.strip();
        if (!wanted.isEmpty()) {
            for (String descendant : settings.descendants(sheet.id())) {
                lines.addAll(audit.lines(descendant, wanted));
            }
            Person person = people.findByName(sheet.id(), wanted);
            if (person != null && person.name().strip().toLowerCase(Locale.ROOT)
                    .equals(wanted.toLowerCase(Locale.ROOT))) {
                String origin = people.origin(sheet.id(), person.id());
                if (origin != null) {
                    lines.addAll(audit.lines(origin, wanted));
                }
            }
        }
        response.send(lines);
    }
}
