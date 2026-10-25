package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The selected claims (at least one), the person to move them to and the user moving them. */
public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassignIds> ids, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.ids() != null) {
            request.ids().stream().filter(Objects::nonNull).forEach(selected::add);
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("Select the claims to move");
        }
        if (request.personId() == null) {
            throw new BadRequestException("Choose the person to move the claims to");
        }
        ids.set(new BulkReassignIds(selected, request.personId()));
        changedBy.set(by);
    }
}
