package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, @Val List<Claim> claims, ClaimLogRepository logs, ClaimLockRepository locks,
            ObjectResponse<List<ClaimResponse>> response) {
        Map<Long, List<ClaimLogLine>> log = logs.findAll(sheet.id());
        Map<Long, String> locked = locks.findAll(sheet.id());
        response.send(claims.stream()
                .map((c) -> ClaimResponse.from(c, log.getOrDefault(c.id(), List.of()), locked.get(c.id()))).toList());
    }
}
