package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The number of the sheet's last bulk settle batch; with an as-of date, the last one made by the end of it. */
public class LoadSettleBatch {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Sheets sheets, AsOfView view,
            ObjectResponse<SettleBatchResponse> response) {
        LocalDate date = AsOfView.parse(asOf);
        int last = date == null ? sheets.lastSettleBatch(sheetId)
                : view.claims(sheetId, date).stream().map(ClaimResponse::batch).filter(b -> !b.isEmpty())
                        .mapToInt(Integer::parseInt).max().orElse(0);
        response.send(new SettleBatchResponse(last == 0 ? "" : String.valueOf(last)));
    }
}
