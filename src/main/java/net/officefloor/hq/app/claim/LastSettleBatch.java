package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The number of the sheet's last bulk settle batch (null: there has been none). */
public class LastSettleBatch {
    public record Result(Integer last) {
    }

    public void last(@PathVariable("sheetId") String sheetId, SheetRepository sheets,
            ObjectResponse<Result> response) {
        response.send(new Result(sheets.lastSettleBatch(sheetId.trim())));
    }
}
