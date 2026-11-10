package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkSettle {
    public void validate(@RequestBody BulkSettleRequest request, Out<BulkSettleRequest> body) {
        List<String> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        body.set(new BulkSettleRequest(ids));
    }
}
