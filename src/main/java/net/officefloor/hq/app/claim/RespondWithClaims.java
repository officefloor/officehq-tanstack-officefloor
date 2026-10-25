package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val SheetResponse sheet, ClaimRepository repository, ClaimLogRepository log,
            ObjectResponse<List<ClaimResponse>> response) {
        java.util.Map<Long, List<String>> lines = log.lines(sheet.id());
        response.send(repository.list(sheet.id()).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()))).toList());
    }
}
