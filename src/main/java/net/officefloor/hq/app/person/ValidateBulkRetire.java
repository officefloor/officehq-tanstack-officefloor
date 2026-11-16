package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body by the row's retire rules (see ValidateRetire): the date is blank or yyyy-MM-dd; the selected person
 *  ids each once (a missing list is none). */
public class ValidateBulkRetire {
    public void validate(@RequestBody BulkRetireRequest request, Out<BulkRetireValues> values) throws InvalidPersonException {
        Retirement[] retirement = new Retirement[1];
        new ValidateRetire().validate(request == null ? null : new RetireRequest(request.from(), request.by()),
                (r) -> retirement[0] = r);
        List<Long> ids = request == null || request.personIds() == null ? List.of()
                : request.personIds().stream().filter(Objects::nonNull).distinct().toList();
        values.set(new BulkRetireValues(ids, retirement[0]));
    }
}
