package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every tag ever used on the sheet's claims, sorted. */
public class LoadTags {
    public void service(@PathVariable("sheetId") String sheetId, Claims claims, ObjectResponse<List<String>> response) {
        response.send(claims.tagsUsed(sheetId));
    }
}
