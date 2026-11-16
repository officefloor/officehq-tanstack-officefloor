package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the selected claim ids (each once; a missing list is none) and who settles them. */
public class ValidateBulkSettle {
    public void validate(@RequestBody BulkSettleRequest request, Out<BulkSettleRequest> body) {
        List<Long> ids = request == null || request.claimIds() == null ? List.of()
                : request.claimIds().stream().filter(Objects::nonNull).distinct().toList();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        body.set(new BulkSettleRequest(ids, by));
    }
}
