package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Opens each selected claim in list order (people in the order they were added, newest claim first within a
 * person), so they are numbered in that order; one that cannot be opened is left as it is and counted refused.
 */
public class BulkOpenClaims {
    public void open(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkOpenRequest request, Claims claims, People people, ClaimStatusChanger changer,
            ObjectResponse<BulkOpenResponse> response) {
        Map<Long, Integer> personOrder = new HashMap<>();
        for (Person person : people.list(sheet)) {
            personOrder.put(person.id(), personOrder.size());
        }
        List<Claim> selected = new ArrayList<>();
        int refused = 0;
        for (Long id : new LinkedHashSet<>(request == null || request.claims() == null ? List.of() : request.claims())) {
            Claim claim = id == null ? null : claims.find(sheet, id);
            if (claim == null) {
                refused++;
            } else {
                selected.add(claim);
            }
        }
        // Newest first within a person, as the sheet lists them.
        Map<Long, Integer> listOrder = new HashMap<>();
        for (Claim claim : claims.list(sheet)) {
            listOrder.put(claim.id(), listOrder.size());
        }
        selected.sort(Comparator.comparing((Claim c) -> personOrder.getOrDefault(c.person(), Integer.MAX_VALUE))
                .thenComparing((Claim c) -> listOrder.getOrDefault(c.id(), Integer.MAX_VALUE)));
        int opened = 0;
        for (Claim claim : selected) {
            try {
                // Re-read so each claim is opened as the store holds it after the ones before it.
                changer.change(sheet, user, claims.find(sheet, claim.id()), ClaimStatusChange.OPEN);
                opened++;
            } catch (InvalidClaimStatusChangeException refusal) {
                refused++;
            }
        }
        response.send(new BulkOpenResponse(opened, refused, claims.list(sheet)));
    }
}
