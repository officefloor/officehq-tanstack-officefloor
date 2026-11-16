package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the selected claim ids (each once; a missing list is none), the new date (a re-dating needs a real
 *  date, yyyy-MM-dd) and who re-dates them. */
public class ValidateBulkRedate {
    public void validate(@RequestBody BulkRedateRequest request, Out<BulkRedating> body)
            throws InvalidClaimStatusException {
        String date = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusException("A re-dating needs the claims' new date");
        }
        List<Long> ids = request.claimIds() == null ? List.of()
                : request.claimIds().stream().filter(Objects::nonNull).distinct().toList();
        String by = request.by() == null ? "" : request.by().trim();
        body.set(new BulkRedating(ids, new ClaimRedating(parsed, by, null)));
    }
}
