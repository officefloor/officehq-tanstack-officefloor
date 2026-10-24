package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository, ObjectResponse<List<ClaimResponse>> response) {
        response.send(repository.findBySheetIdOrderByListedDescIdDesc(sheet.getId()).stream().map(ClaimResponse::from).toList());
    }
}
