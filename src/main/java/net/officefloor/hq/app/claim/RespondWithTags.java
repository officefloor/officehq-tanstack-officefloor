package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithTags {
    public void respond(@PathVariable("sheetId") String sheetId, Claims claims, ObjectResponse<List<String>> response) {
        response.send(claims.tagsUsed(sheetId.trim()));
    }
}
