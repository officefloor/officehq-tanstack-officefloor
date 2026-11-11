package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a retirement date that is not a date, before anything is read. */
public class ValidateBulkRetire {
    public void validate(@RequestBody BulkRetireRequest request, Out<BulkRetireRequest> body)
            throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("A retirement date is a date");
            }
        }
        List<String> ids = request == null || request.personIds() == null ? List.of() : request.personIds();
        body.set(new BulkRetireRequest(ids, from));
    }
}
