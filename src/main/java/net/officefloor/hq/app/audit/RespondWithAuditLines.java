package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Responds with the sheet's audit lines exactly as the file holds them, newest first. A name keeps only the lines
 * about that person: those carrying name=<the name> or person=<the name>, compared as names are (case and surrounding
 * spaces ignored). The viewer reads the file; it records nothing.
 */
public class RespondWithAuditLines {

    /** A name= or person= field; its value runs to the next " key=" field or the end of the line (names may hold spaces). */
    private static final Pattern PERSON_FIELD = Pattern.compile("(?:^| )(?:name|person)=(.*?)(?= [^ =]+=|$)");

    public void respond(@PathVariable("sheet") String sheet, @RequestParam(value = "name", required = false) String name,
            Audit audit, ObjectResponse<List<String>> response) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines(sheet)) {
            if (key.isEmpty() || isAbout(line, key)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        response.send(lines);
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
