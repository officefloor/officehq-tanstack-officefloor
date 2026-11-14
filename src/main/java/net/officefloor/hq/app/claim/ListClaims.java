package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListClaims {
    public void list(@PathVariable("sheetId") String sheetId, ClaimRepository claims,
            ObjectResponse<List<Claim>> response) {
        response.send(claims.list(sheetId.trim()));
    }
}
