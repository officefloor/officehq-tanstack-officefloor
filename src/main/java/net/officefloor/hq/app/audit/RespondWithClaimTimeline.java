package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** A claim's timeline: the sheet's audit lines about the claim as the file holds them, newest first. For a claim with
 *  a number, the lines carrying number=<its number>; for a claim with none, the lines carrying no number that name its
 *  person (person=, compared as names are), its amount and its date as the screen shows them. Reads the file; keeps
 *  no copy. */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, Audit audit, PersonRepository people, ObjectResponse<List<String>> response) {
        ClaimResponse shown = ClaimResponse.from(claim);
        Person person = people.find(claim.sheetId(), claim.personId());
        String name = person == null ? null : person.name().trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (String line : audit.lines()) {
            if (!field(line, "sheet", claim.sheetId())) {
                continue;
            }
            boolean about = claim.number() != null
                    ? field(line, "number", claim.number())
                    : name != null && !line.contains(" number=") && field(line.toLowerCase(Locale.ROOT), "person", name)
                            && field(line, "amount", shown.amount()) && field(line, "date", shown.date());
            if (about) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        response.send(lines);
    }

    /** Whether a <label>= field of the line holds exactly the value. */
    private static boolean field(String line, String label, String value) {
        String key = " " + label + "=" + value;
        int at = line.indexOf(key);
        while (at >= 0) {
            int end = at + key.length();
            if (end == line.length() || line.charAt(end) == ' ') {
                return true;
            }
            at = line.indexOf(key, at + 1);
        }
        return false;
    }
}
