package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet; as of a date (asOf), as it stood at the end of it: that date its today, the date itself, and its last
 *  bulk settle batch then (the highest batch its claims then carried). */
public class RespondWithSheet {

    /** @param asOf the date the sheet is shown as of (null: as it stands now).
     *  @param lastBatch the number of the sheet's last bulk settle batch (null: none yet). */
    public record SheetResponse(String id, String today, boolean archived, String asOf, Integer lastBatch) {
    }

    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<SheetResponse> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Sheet view = asOf.sheet(sheet, date);
        Integer lastBatch = date == null
                ? (sheet.getLastSettleBatch() == 0 ? null : sheet.getLastSettleBatch())
                : asOf.claims(sheet.getId(), date).stream().map(Claim::getSettleBatch).filter(Objects::nonNull)
                        .max(Integer::compare).orElse(null);
        response.send(new SheetResponse(view.getId(), view.getToday().toString(), view.isArchived(),
                date == null ? null : date.toString(), lastBatch));
    }
}
