package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The selected claims and the void's reason, kept trimmed; null when none was chosen. Whether it is needed is the void's rule. */
public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoidRequest> body) {
        List<String> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        body.set(new BulkVoidRequest(ids, reason.isEmpty() ? null : reason));
    }
}
