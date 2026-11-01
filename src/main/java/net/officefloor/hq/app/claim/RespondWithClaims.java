package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every claim on the sheet, newest first. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheet") String sheet, Claims claims, ObjectResponse<List<Claim>> response) {
        response.send(claims.list(sheet));
    }
}
