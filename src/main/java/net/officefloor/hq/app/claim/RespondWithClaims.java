package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository, ClaimLogRepository log,
            ObjectResponse<ClaimsResponse> response) {
        Map<Long, List<String>> logs = log.findAll(sheet.id());
        response.send(new ClaimsResponse(repository.findAll(sheet.id()).stream()
                .map((claim) -> ClaimResponse.from(claim, logs.getOrDefault(claim.getId(), List.of()))).toList()));
    }
}
