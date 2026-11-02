package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Responds with the sheet's audit lines exactly as the file holds them, newest first. A name keeps only the lines
 * about that person: those carrying name=<the name> or person=<the name>, compared as names are (case and surrounding
 * spaces ignored). A name also lists the lines about the person on each of the sheet's descendants (the sheets below it
 * by parent), after the sheet's own: each sheet's lines newest first, the sheets by id. When the name is that of a person on the sheet who was transferred to it, the lines about them on
 * the sheet they came from follow, newest first, each as that sheet's file holds it. The viewer reads the file; it
 * records nothing.
 */
public class RespondWithAuditLines {

    /** A name= or person= field; its value runs to the next " key=" field or the end of the line (names may hold spaces). */
    private static final Pattern PERSON_FIELD = Pattern.compile("(?:^| )(?:name|person)=(.*?)(?= [^ =]+=|$)");

    public void respond(@PathVariable("sheet") String sheet, @RequestParam(value = "name", required = false) String name,
            Audit audit, People people, Settings settings, ObjectResponse<List<String>> response) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> lines = about(audit, sheet, key);
        if (!key.isEmpty()) {
            List<String> descendants = new ArrayList<>(settings.family(sheet));
            descendants.remove(sheet);
            Collections.sort(descendants);
            for (String descendant : descendants) {
                lines.addAll(about(audit, descendant, key));
            }
            Person person = people.findByName(sheet, key);
            if (person != null && person.origin() != null && person.name().toLowerCase(Locale.ROOT).equals(key)) {
                lines.addAll(about(audit, person.origin(), key));
            }
        }
        response.send(lines);
    }

    /** The sheet's lines about the person (every line when the key is blank), newest first. */
    private static List<String> about(Audit audit, String sheet, String key) {
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines(sheet)) {
            if (key.isEmpty() || isAbout(line, key)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    private static boolean isAbout(String line, String key) {
        Matcher m = PERSON_FIELD.matcher(line);
        while (m.find()) {
            if (m.group(1).trim().toLowerCase(Locale.ROOT).equals(key)) {
                return true;
            }
        }
        return false;
    }
}
