package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk retire with no one selected or a date not a date. */
public class ValidateBulkRetire {
    public void validate(@RequestBody BulkRetireRequest request, Out<BulkRetireRequest> body)
            throws InvalidPersonException {
        List<Long> ids = request == null || request.personIds() == null ? List.of()
                : request.personIds().stream().filter((id) -> id != null).toList();
        if (ids.isEmpty()) {
            throw new InvalidPersonException("Select the people to retire");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + from);
            }
        }
        body.set(new BulkRetireRequest(ids, from.isEmpty() ? null : from));
    }
}
