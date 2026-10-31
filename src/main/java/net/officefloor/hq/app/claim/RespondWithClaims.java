package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, Claims claims, Figures figures,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(claims.onSheet(sheetId).stream().map(c -> ClaimResponse.from(c, figures)).toList());
    }
}
