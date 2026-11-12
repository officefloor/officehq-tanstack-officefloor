package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the audit file holds them, newest first, optionally only those about a person;
 *  with a person, followed by the lines about them on the sheet's descendants (each sheet's newest first, sheets
 *  by id), then, for a person transferred here, the lines about them from the sheet they came from. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "person", required = false) String person, Audit audit, PersonStore people, SettingStore settings,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        List<String> lines = new ArrayList<>(audit.lines(sheet, person));
        if (person != null && !person.trim().isEmpty()) {
            List<String> descendants = new ArrayList<>(settings.descendants(sheet));
            java.util.Collections.sort(descendants);
            for (String descendant : descendants) {
                lines.addAll(audit.lines(descendant, person));
            }
            Person found = people.findByName(sheet, person.trim());
            if (found != null && found.getOrigin() != null) {
                lines.addAll(audit.linesBefore(found.getOrigin(), sheet, person));
            }
        }
        response.send(lines);
    }
}
