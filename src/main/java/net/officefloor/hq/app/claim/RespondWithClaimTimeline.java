package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Responds with a claim's timeline: the sheet's audit lines about it, newest first, exactly as the file holds them.
 * A numbered claim's lines are those carrying number=<its number (as it shows)>; a claim with no number has the lines
 * naming its person (compared as names are), amount and date together that carry no number. It reads the file; it
 * records nothing.
 */
public class RespondWithClaimTimeline {

    /** A key=value field; its value runs to the next " key=" field or the end of the line (names may hold spaces). */
    private static final Pattern FIELD = Pattern.compile("(?:^| )([^ =]+)=(.*?)(?= [^ =]+=|$)");

    public void respond(@PathVariable("sheet") String sheet, @Val Claim claim, Audit audit, People people,
            ObjectResponse<List<String>> response) {
        String number = claim.shownNumber();
        Person person = people.find(sheet, claim.person());
        String name = person == null ? null : person.name().trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines(sheet)) {
            Map<String, String> fields = fields(line);
            boolean about = number != null
                    ? number.equals(fields.get("number"))
                    : !fields.containsKey("number") && name != null
                            && fields.get("person") != null
                            && name.equals(fields.get("person").trim().toLowerCase(Locale.ROOT))
                            && claim.amount().equals(fields.get("amount")) && claim.date().equals(fields.get("date"));
            if (about) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        response.send(lines);
    }

    /** The line's fields by key; a key given twice keeps its first value. */
    private static Map<String, String> fields(String line) {
        Map<String, String> fields = new HashMap<>();
        Matcher m = FIELD.matcher(line);
        while (m.find()) {
            fields.putIfAbsent(m.group(1), m.group(2));
        }
        return fields;
    }
}
