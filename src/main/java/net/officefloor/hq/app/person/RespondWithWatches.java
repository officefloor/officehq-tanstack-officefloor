package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId, @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "asOf", required = false) String asOf, Watches watches, AsOf asOfSheet,
            ObjectResponse<List<Long>> response) throws InvalidSheetRequestException {
        response.send(user == null || user.isBlank() ? List.of()
                : asOfSheet.view(sheetId, asOf, () -> watches.watchedBy(sheetId, user.trim())));
    }
}
