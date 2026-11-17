package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the selected claim ids (each once; a missing list is none), the void reason (none is empty) and who
 *  voids them. */
public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoidRequest> body) {
        List<Long> ids = request == null || request.claimIds() == null ? List.of()
                : request.claimIds().stream().filter(Objects::nonNull).distinct().toList();
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        body.set(new BulkVoidRequest(ids, reason, by));
    }
}
