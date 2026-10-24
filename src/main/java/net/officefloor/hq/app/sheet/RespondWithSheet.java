package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet; as of a date (asOf), as it stood at the end of it: that date its today, and the date itself. */
public class RespondWithSheet {

    /** @param asOf the date the sheet is shown as of (null: as it stands now). */
    public record SheetResponse(String id, String today, boolean archived, String asOf) {
    }

    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<SheetResponse> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Sheet view = asOf.sheet(sheet, date);
        response.send(new SheetResponse(view.getId(), view.getToday().toString(), view.isArchived(),
                date == null ? null : date.toString()));
    }
}
