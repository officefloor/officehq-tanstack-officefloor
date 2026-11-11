package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimStore store,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(store.list(sheetId.trim()).stream().map(ClaimResponse::from).toList());
    }
}
