package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkGradeRequest request,
            Out<BulkGradeInput> body, SheetStore sheets, GradeStore grades) throws InvalidSheetException {
        sheets.refuseArchived(sheetId.trim());
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetException("Select the people to grade");
        }
        grades.refuseUnknown(sheetId.trim(), request.grade());
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        try {
            if (!from.isEmpty()) {
                LocalDate.parse(from);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + from);
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
        List<Long> list = new ArrayList<>(ids);
        body.set(new BulkGradeInput(list, request.grade(), from.isEmpty() ? null : from, request.user()));
    }
}
