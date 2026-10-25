package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The selected claims (at least one) and the user opening them. */
public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpenIds> ids, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.ids() != null) {
            request.ids().stream().filter(Objects::nonNull).forEach(selected::add);
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("Select the claims to open");
        }
        ids.set(new BulkOpenIds(selected));
        changedBy.set(by);
    }
}
