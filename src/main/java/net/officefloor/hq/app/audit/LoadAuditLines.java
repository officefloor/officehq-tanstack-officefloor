package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines, newest first. Filtered by a name, the lines about that name in each of the sheet's
 *  descendants follow (each sheet's newest first, sheets by id); then, for a person transferred here, the lines
 *  about them from the sheet they came from (newest first, exactly as the file holds them). */
public class LoadAuditLines {
    public void load(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, ServerHttpConnection connection,
            Audit audit, PersonRepository people, SettingsRepository settings, Out<AuditLines> lines) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        List<String> found = new ArrayList<>(audit.read(id, name));
        String who = name == null ? "" : name.trim();
        if (!who.isEmpty()) {
            List<String> descendants = new ArrayList<>(settings.descendants(id));
            descendants.sort(null);
            for (String descendant : descendants) found.addAll(audit.read(descendant, who));
            Person person = people.findByName(id, who);
            if (person != null && person.origin() != null
                    && person.name().trim().toLowerCase(Locale.ROOT).equals(who.toLowerCase(Locale.ROOT))) {
                found.addAll(audit.read(person.origin(), who));
            }
        }
        lines.set(new AuditLines(found));
    }
}
