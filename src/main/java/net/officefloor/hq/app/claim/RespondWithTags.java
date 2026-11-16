package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every tag ever used on the sheet's claims, sorted (see ClaimRepository#tagsUsed). */
public class RespondWithTags {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository claims, ObjectResponse<List<String>> response) {
        response.send(claims.tagsUsed(sheetId.trim()));
    }
}
