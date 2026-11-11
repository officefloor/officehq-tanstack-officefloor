package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a hold's dates that are not dates, or a to-date before the from-date, before anything is read. */
public class ValidateBulkHold {
    public void validate(@RequestBody BulkHoldRequest request, Out<BulkHoldRequest> body)
            throws InvalidPersonException {
        String from = trim(request == null ? null : request.from());
        String to = trim(request == null ? null : request.to());
        LocalDate fromDate = date(from, "A hold's from-date is a date");
        LocalDate toDate = date(to, "A hold's to-date is a date");
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new InvalidPersonException("A hold's to-date is not before its from-date");
        }
        List<String> ids = request == null || request.personIds() == null ? List.of() : request.personIds();
        body.set(new BulkHoldRequest(ids, from, to));
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocalDate date(String raw, String message) throws InvalidPersonException {
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException(message);
        }
    }
}
