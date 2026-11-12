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

public class ValidateBulkRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkRetireRequest request,
            Out<BulkRetireInput> body, SheetStore sheets) throws InvalidSheetException {
        String sheet = sheetId.trim();
        sheets.refuseArchived(sheet);
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetException("Select the people to retire");
        }
        String dateText = request.date() == null ? "" : request.date().trim();
        LocalDate from;
        try {
            from = dateText.isEmpty() ? sheets.today(sheet) : LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + dateText);
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
        body.set(new BulkRetireInput(new ArrayList<>(ids), from, request.user()));
    }
}
