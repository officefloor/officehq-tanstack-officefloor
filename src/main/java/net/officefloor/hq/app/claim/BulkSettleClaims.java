package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Settles each selected claim in list order (people by their list positions, newest claim first within a
 * person) by the rules of settling one claim, as the signed-in user; one a clerk's settle sends to PENDING counts as
 * settled. It is all or nothing: when any selected claim cannot be settled none is, every one counts refused and
 * nothing is written. Otherwise the bulk settle is the sheet's next batch, each claim it settled carries the batch's
 * number, and after the claims' own lines a BATCH_SETTLED line gives its count and the frozen figures of those
 * SETTLED added up.
 */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkSettleRequest request, Claims claims, People people, ClaimStatusChanger changer,
            SettleBatches batches, Audit audit,
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
        // All or nothing: when any selected claim cannot be settled by the user, none is and every one counts refused.
        boolean refusal = refused > 0;
        for (Claim claim : selected) {
            try {
                changer.vet(sheet, user, claim, ClaimStatusChange.SETTLE, null);
            } catch (InvalidClaimStatusChangeException e) {
                refusal = true;
            }
        }
        if (refusal) {
            response.send(new BulkSettleResponse(0, refused + selected.size(), claims.list(sheet), batches.last(sheet)));
            return;
        }
        int settled = 0;
        Integer batch = null;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : selected) {
            try {
                // Re-read so each claim is settled as the store holds it after the ones before it.
                changer.change(sheet, user, claims.find(sheet, claim.id()), ClaimStatusChange.SETTLE);
                if (batch == null) {
                    batch = batches.next(sheet, user);
                }
                claims.setBatch(sheet, claim.id(), batch);
                settled++;
                Claim after = claims.find(sheet, claim.id());
                if (after.status() == ClaimStatus.SETTLED && after.settledDue() != null) {
                    total = total.add(new BigDecimal(after.settledDue()));
                }
            } catch (InvalidClaimStatusChangeException lost) {
                refused++;
            }
        }
        if (batch != null) {
            audit.record("BATCH_SETTLED sheet=" + sheet + " by=" + user + " batch=" + batch + " count=" + settled
                    + " total=" + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        response.send(new BulkSettleResponse(settled, refused, claims.list(sheet), batches.last(sheet)));
    }
}
