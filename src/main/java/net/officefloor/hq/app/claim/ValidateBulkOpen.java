package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpenRequest> body) {
        List<String> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        body.set(new BulkOpenRequest(ids));
    }
}
