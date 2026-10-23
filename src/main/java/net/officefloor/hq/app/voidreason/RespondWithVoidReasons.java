package net.officefloor.hq.app.voidreason;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's void reasons in the order they were added, each with how many VOID claims were voided with it (as
 *  of a date (?asOf=): the claims VOID as the sheet stood at the end of it). */
public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, VoidReasonRepository repository,
            ClaimRepository claims, ObjectResponse<List<VoidReasonResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        Map<String, Integer> voided = end == null ? repository.voided(id)
                : repository.voided(id, claims.listAsOf(id, end).stream().filter(c -> c.status() == net.officefloor.hq.app.claim.ClaimStatus.VOID)
                        .map(c -> c.id()).collect(Collectors.toSet()));
        response.send(repository.list(id).stream()
                .map(r -> VoidReasonResponse.from(r, voided.getOrDefault(r.reason(), 0))).toList());
    }
}
