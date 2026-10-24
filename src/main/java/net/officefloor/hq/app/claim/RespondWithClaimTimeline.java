package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The claim's timeline: the sheet's audit lines about the claim exactly as the file holds them, newest first. A
 *  numbered claim's lines are those carrying number=&lt;its number&gt;; an unnumbered claim's are those naming its
 *  person, amount and date together and carrying no number. */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, PersonRepository people, Audit audit,
            ObjectResponse<List<String>> response) {
        List<String> wanted;
        if (claim.getNumber() != null && !claim.getNumber().isBlank()) {
            wanted = List.of("number=" + claim.getNumber());
        } else {
            String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
            wanted = List.of("person=" + person, "amount=" + claim.getAmount().setScale(2).toPlainString(),
                    "date=" + claim.getDate());
        }
        boolean numbered = wanted.size() == 1;
        List<String> lines = new ArrayList<>(audit.linesOf(claim.getSheetId()).stream().filter(line -> {
            List<String> fields = Arrays.asList(line.split(" "));
            return fields.containsAll(wanted)
                    && (numbered || fields.stream().noneMatch(f -> f.startsWith("number=")));
        }).toList());
        Collections.reverse(lines);
        response.send(lines);
    }
}
