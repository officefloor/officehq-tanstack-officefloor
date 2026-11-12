package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RetireRequest request,
            SheetStore sheets, Out<RetireInput> body) throws InvalidSheetException {
        String dateText = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate from;
        try {
            from = dateText.isEmpty() ? sheets.today(sheetId.trim()) : LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + dateText);
        }
        body.set(new RetireInput(from, request == null ? null : request.user()));
    }
}
