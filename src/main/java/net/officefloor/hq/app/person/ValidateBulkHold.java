package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body by the row's hold rules (see ValidateHold): each date is blank or yyyy-MM-dd; the selected person ids
 *  each once (a missing list is none). */
public class ValidateBulkHold {
    public void validate(@RequestBody BulkHoldRequest request, Out<BulkHoldValues> values) throws InvalidPersonException {
        Hold[] hold = new Hold[1];
        new ValidateHold().validate(request == null ? null : new HoldRequest(request.from(), request.to(), request.by()),
                (h) -> hold[0] = h);
        List<Long> ids = request == null || request.personIds() == null ? List.of()
                : request.personIds().stream().filter(Objects::nonNull).distinct().toList();
        values.set(new BulkHoldValues(ids, hold[0]));
    }
}
