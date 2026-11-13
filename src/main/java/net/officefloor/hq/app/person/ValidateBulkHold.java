package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkHold {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkHoldRequest request,
            Out<BulkHoldInput> body, SheetStore sheets) throws InvalidSheetException {
        String sheet = sheetId.trim();
        sheets.refuseArchived(sheet);
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetException("Select the people to hold");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        String toText = request.to() == null ? "" : request.to().trim();
        LocalDate from = fromText.isEmpty() ? sheets.today(sheet) : parse(fromText);
        LocalDate to = toText.isEmpty() ? null : parse(toText);
        if (to != null && to.isBefore(from)) {
            throw new InvalidSheetException("The hold ends before it starts");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : request.personIds()) {
            String id = raw == null ? "" : raw.trim();
            try {
                ids.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                throw new InvalidSheetException("No such person: " + id);
            }
        }
        body.set(new BulkHoldInput(new ArrayList<>(ids), new Hold(from, to), request.user()));
    }

    private static LocalDate parse(String text) throws InvalidSheetException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
