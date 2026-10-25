package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Loads the sheet for a read, as {@link LoadOrCreateSheet}; with a preview date (preview), the sheet as the screen
 *  previews it: its records as they are now, that date standing in for its today (see {@link Sheet#previewedOn}).
 *  Only reads take a preview: a change always uses the sheet's real today. */
public class LoadOrCreateSheetForRead {
    public void load(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "preview", required = false) String preview, SheetRepository repository,
            Out<Sheet> sheetOut) throws InvalidTodayException {
        String id = sheetId.trim();
        Sheet sheet = repository.findById(id).orElseGet(() -> repository.save(new Sheet(id)));
        LocalDate date = AsOf.dateOf(preview);
        sheetOut.set(date == null ? sheet : sheet.previewedOn(date));
    }
}
