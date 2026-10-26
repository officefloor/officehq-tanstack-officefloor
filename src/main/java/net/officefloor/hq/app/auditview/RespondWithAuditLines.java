package net.officefloor.hq.app.auditview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sheet's audit lines exactly as the file holds them, newest first. A name keeps only the lines about that
 * person: those carrying name=<the name> or person=<the name>, compared as names are (case and surrounding spaces
 * ignored). A field's value runs to the next " key=" field. With a name, the lines about them on each of the
 * sheet's descendants follow, each sheet's newest first, the sheets by id. When the name is a person on the sheet who was
 * transferred in, the lines about them from the sheet they came from follow, newest first, as that sheet's file
 * holds them.
 */
public class RespondWithAuditLines {

    private static final Pattern FIELD = Pattern.compile("(?:^| )(name|person)=(.*?)(?= [^\\s=]+=|$)");

    public void respond(@Val SheetResponse sheet, @RequestParam(value = "name", required = false) String name,
            Audit audit, PersonRepository people, RateRepository rates, ObjectResponse<List<String>> response) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> lines = about(audit.lines(sheet.id()), wanted.isEmpty() ? Set.of() : Set.of(wanted));
        if (!wanted.isEmpty()) {
            List<String> descendants = new ArrayList<>(rates.descendantsOf(sheet.id()));
            Collections.sort(descendants);
            for (String descendant : descendants) {
                lines.addAll(about(audit.lines(descendant), Set.of(wanted)));
            }
            Person person = people.findByName(sheet.id(), wanted);
            if (person != null && person.getOrigin() != null) {
                Set<String> names = new HashSet<>(List.of(wanted, person.getName().trim().toLowerCase(Locale.ROOT)));
                lines.addAll(about(audit.lines(person.getOrigin()), names));
            }
        }
        response.send(lines);
    }

    /** The lines about any of the names (all of them when none), newest first. */
    private static List<String> about(List<String> all, Set<String> names) {
        List<String> lines = new ArrayList<>();
        for (String line : all) {
            if (names.isEmpty() || isAbout(line, names)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    private static boolean isAbout(String line, Set<String> names) {
        Matcher field = FIELD.matcher(line);
        while (field.find()) {
            if (names.contains(field.group(2).trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
