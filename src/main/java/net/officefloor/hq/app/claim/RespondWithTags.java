package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every tag ever used on the sheet's claims, sorted: kept even once no claim carries it. */
public class RespondWithTags {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository,
            ObjectResponse<List<String>> response) {
        response.send(repository.tagsUsed(sheetId.trim()));
    }
}
