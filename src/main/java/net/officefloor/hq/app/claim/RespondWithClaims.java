package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository, ClaimLogRepository log,
            ObjectResponse<List<ClaimResponse>> response) {
        Map<Long, List<String>> lines = log.linesOnSheet(sheet.getId());
        response.send(repository.findBySheetIdOrderByListedDescIdDesc(sheet.getId()).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()))).toList());
    }
}
