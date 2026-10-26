package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The selected claims (at least one), the void reason chosen (if any) and the user voiding them. */
public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoidIds> ids, Out<VoidChoice> voidChoice,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.ids() != null) {
            request.ids().stream().filter(Objects::nonNull).forEach(selected::add);
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("Select the claims to void");
        }
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        ids.set(new BulkVoidIds(selected));
        voidChoice.set(new VoidChoice(reason.isEmpty() ? null : reason));
        changedBy.set(by);
    }
}
