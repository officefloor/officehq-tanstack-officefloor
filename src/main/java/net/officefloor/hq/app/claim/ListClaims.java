package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListClaims {
    public void list(@PathVariable("sheetId") String sheetId, ClaimRepository claims, ClaimLogRepository log,
            ObjectResponse<List<Claim>> response) {
        String id = sheetId.trim();
        response.send(log.withLogs(id, claims.list(id)));
    }
}
