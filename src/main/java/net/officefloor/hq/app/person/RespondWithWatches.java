package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId, @RequestParam(value = "user", required = false) String user,
            Watches watches, ObjectResponse<List<Long>> response) {
        response.send(user == null || user.isBlank() ? List.of() : watches.watchedBy(sheetId, user.trim()));
    }
}
