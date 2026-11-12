package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HoldRequest request,
            SheetStore sheets, Out<HoldInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.user());
        String fromText = request == null || request.from() == null ? "" : request.from().trim();
        String toText = request == null || request.to() == null ? "" : request.to().trim();
        LocalDate from = fromText.isEmpty() ? sheets.today(sheetId.trim()) : parse(fromText);
        LocalDate to = toText.isEmpty() ? null : parse(toText);
        if (to != null && to.isBefore(from)) {
            throw new InvalidSheetException("The hold ends before it starts");
        }
        body.set(new HoldInput(new Hold(from, to), request == null ? null : request.user()));
    }

    private static LocalDate parse(String text) throws InvalidSheetException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
