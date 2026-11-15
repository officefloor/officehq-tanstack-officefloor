package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListTags {
    public void list(@PathVariable("sheetId") String sheetId, ClaimRepository claims,
            ObjectResponse<List<String>> response) {
        response.send(claims.tags(sheetId.trim()));
    }
}
