package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A bulk re-dating needs the claims' new date. */
public class ValidateBulkRedate {
    public void validate(@RequestBody BulkRedateRequest request, Out<BulkRedateRequest> body, Out<Redate> date)
            throws InvalidClaimException {
        List<String> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        String raw = request == null || request.date() == null ? "" : request.date().trim();
        try {
            date.set(new Redate(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A re-dating needs the claims' new date");
        }
        body.set(new BulkRedateRequest(ids, raw));
    }
}
