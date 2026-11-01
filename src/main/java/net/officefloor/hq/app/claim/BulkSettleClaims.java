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
 * Settles each selected claim in list order (people in the order they were added, newest claim first within a
 * person) by the rules of settling one claim, as the signed-in user; one a clerk's settle sends to PENDING counts as
 * settled, one that cannot be settled is left as it is and counted refused. When at least one is settled the bulk
 * settle is the sheet's next batch, and each claim it settled carries the batch's number.
 */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkSettleRequest request, Claims claims, People people, ClaimStatusChanger changer,
            SettleBatches batches,
            ObjectResponse<BulkSettleResponse> response) {
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
        int settled = 0;
        Integer batch = null;
        for (Claim claim : selected) {
            try {
                // Re-read so each claim is settled as the store holds it after the ones before it.
                changer.change(sheet, user, claims.find(sheet, claim.id()), ClaimStatusChange.SETTLE);
                if (batch == null) {
                    batch = batches.next(sheet, user);
                }
                claims.setBatch(sheet, claim.id(), batch);
                settled++;
            } catch (InvalidClaimStatusChangeException refusal) {
                refused++;
            }
        }
        response.send(new BulkSettleResponse(settled, refused, claims.list(sheet), batches.last(sheet)));
    }
}
