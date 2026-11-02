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
 * Voids each selected claim in list order (people by their list positions, newest claim first within a
 * person) exactly as its row's void does, with the one reason chosen; one that cannot be voided is left as it is and counted refused.
 */
public class BulkVoidClaims {
    public void voidClaims(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkVoidRequest request, Claims claims, People people, ClaimStatusChanger changer,
            ObjectResponse<BulkVoidResponse> response) {
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
        int voided = 0;
        for (Claim claim : selected) {
            try {
                // Re-read so each claim is voided as the store holds it after the ones before it.
                changer.change(sheet, user, claims.find(sheet, claim.id()), ClaimStatusChange.VOID,
                        request.reason());
                voided++;
            } catch (InvalidClaimStatusChangeException refusal) {
                refused++;
            }
        }
        response.send(new BulkVoidResponse(voided, refused, claims.list(sheet)));
    }
}
