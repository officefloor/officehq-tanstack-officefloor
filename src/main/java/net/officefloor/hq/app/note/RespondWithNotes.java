package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's notes, newest first; as of a date (asOf), only those added while the sheet's today was on or
 *  before it. */
public class RespondWithNotes {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam,
            NoteRepository repository, ObjectResponse<List<NoteResponse>> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        response.send(repository.findBySheetIdOrderByIdDesc(sheet.getId()).stream()
                .filter(n -> date == null || !n.getOnDate().isAfter(date)).map(NoteResponse::from).toList());
    }
}
