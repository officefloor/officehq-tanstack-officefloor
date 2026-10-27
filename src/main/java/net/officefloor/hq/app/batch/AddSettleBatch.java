package net.officefloor.hq.app.batch;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class AddSettleBatch {
    // Only the listed claims on the sheet now SETTLED or PENDING join the batch; with none, no batch is made.
    public void add(@PathVariable("sheetId") String sheetId, @RequestBody(required = false) SettleBatchRequest body,
            Claims claims, SettleBatches batches, ServerHttpConnection connection) {
        String sheet = sheetId.trim();
        List<Long> settled = (body == null || body.claimIds() == null ? List.<Long>of() : body.claimIds()).stream()
                .distinct().map(id -> id == null ? null : claims.find(sheet, id))
                .filter(c -> c != null && (c.status() == ClaimStatus.SETTLED || c.status() == ClaimStatus.PENDING))
                .map(Claim::id).toList();
        if (!settled.isEmpty()) {
            batches.add(sheet, Audit.user(connection).trim(), settled);
        }
    }
}
