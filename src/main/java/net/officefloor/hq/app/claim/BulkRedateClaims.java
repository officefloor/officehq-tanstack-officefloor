package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
 * Re-dates each selected claim to the one date in list order (people by their list positions, newest claim first
 * within a person) by the rules of re-dating one claim, as the signed-in user; one that cannot be re-dated is left as
 * it is and counted refused.
 */
public class BulkRedateClaims {
    public void redate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkRedateRequest request, Claims claims, People people, ClaimRedater redater,
            ObjectResponse<BulkRedateResponse> response) {
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
        LocalDate date = null;
        try {
            if (request != null && request.date() != null && !request.date().isBlank()) {
                date = LocalDate.parse(request.date().trim());
            }
        } catch (DateTimeParseException e) {
            date = null;
        }
        int redated = 0;
        for (Claim claim : selected) {
            try {
                if (date == null) {
                    throw new InvalidClaimStatusChangeException("A redate names the claims' new date as yyyy-mm-dd");
                }
                // Re-read so each claim is re-dated as the store holds it after the ones before it.
                redater.redate(sheet, user, claims.find(sheet, claim.id()), date);
                redated++;
            } catch (InvalidClaimStatusChangeException refusal) {
                refused++;
            }
        }
        response.send(new BulkRedateResponse(redated, refused, claims.list(sheet)));
    }
}
