package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk hold with no one selected, a date that is not a date, or a to-date before the
 *  from-date; binds blank dates as null. */
public class ValidateBulkHold {
    public void validate(@RequestBody BulkHoldRequest request, Out<BulkHoldRequest> body)
            throws InvalidPersonException {
        List<Long> ids = request == null || request.personIds() == null ? List.of()
                : request.personIds().stream().filter((id) -> id != null).toList();
        if (ids.isEmpty()) {
            throw new InvalidPersonException("Select the people to hold");
        }
        String from = date(request.from());
        String to = date(request.to());
        if (from != null && to != null && to.compareTo(from) < 0) {
            throw new InvalidPersonException("The hold ends before it starts: " + from + " to " + to);
        }
        body.set(new BulkHoldRequest(ids, from, to));
    }

    private static String date(String text) throws InvalidPersonException {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a date: " + value);
        }
    }
}
