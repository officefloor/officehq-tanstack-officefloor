package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * A claim's timeline: the sheet's audit lines about it exactly as the file holds them, newest first. A numbered
 * claim's lines are those carrying number=<its number>; an unnumbered claim's are those, carrying no number, naming
 * its person, amount and date together. A field's value runs to the next " key=" field.
 */
public class RespondWithClaimTimeline {

    private static final Pattern FIELD = Pattern.compile("(?:^| )([^\\s=]+)=(.*?)(?= [^\\s=]+=|$)");

    public void respond(@Val SheetResponse sheet, @Val Claim claim, Audit audit, PersonRepository people,
            ObjectResponse<List<String>> response) {
        Person person = people.find(sheet.id(), claim.getPersonId());
        String number = claim.getNumber();
        String amount = claim.getAmount().setScale(2).toPlainString();
        String date = String.valueOf(claim.getDate());
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines(sheet.id())) {
            Map<String, String> fields = fields(line);
            boolean about = number != null
                    ? number.equals(fields.get("number"))
                    : !fields.containsKey("number") && person != null
                            && person.getName().equals(fields.get("person")) && amount.equals(fields.get("amount"))
                            && date.equals(fields.get("date"));
            if (about) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        response.send(lines);
    }

    /** The line's fields, the first of each key. */
    private static Map<String, String> fields(String line) {
        Map<String, String> fields = new HashMap<>();
        Matcher field = FIELD.matcher(line);
        while (field.find()) {
            fields.putIfAbsent(field.group(1), field.group(2));
        }
        return fields;
    }
}
