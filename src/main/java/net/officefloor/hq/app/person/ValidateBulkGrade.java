package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkGrade {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkGradeRequest request,
            Out<BulkGradeInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetException("Select the people to grade");
        }
        if (request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
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
