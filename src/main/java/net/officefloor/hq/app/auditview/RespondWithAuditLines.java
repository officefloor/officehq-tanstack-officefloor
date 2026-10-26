package net.officefloor.hq.app.auditview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sheet's audit lines exactly as the file holds them, newest first. A name keeps only the lines about that
 * person: those carrying name=<the name> or person=<the name>, compared as names are (case and surrounding spaces
 * ignored). A field's value runs to the next " key=" field.
 */
public class RespondWithAuditLines {

    private static final Pattern FIELD = Pattern.compile("(?:^| )(name|person)=(.*?)(?= [^\\s=]+=|$)");

    public void respond(@Val SheetResponse sheet, @RequestParam(value = "name", required = false) String name,
            Audit audit, ObjectResponse<List<String>> response) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines(sheet.id())) {
            if (wanted.isEmpty() || isAbout(line, wanted)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        response.send(lines);
    }

    private static boolean isAbout(String line, String wanted) {
        Matcher field = FIELD.matcher(line);
        while (field.find()) {
            if (field.group(2).trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
