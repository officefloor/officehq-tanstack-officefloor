package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk grade the person form would refuse for every person: an unknown grade or a from-date
 *  that is not a date (or an archived sheet). A viewer changes nothing: their bulk grade grades no one. */
public class ValidateBulkGrade {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkGradeRequest request,
            Out<BulkGradeRequest> body, Settings settings) throws InvalidPersonException, ViewerRefusedException {
        boolean viewer = false;
        try {
            settings.refuseViewer(sheetId, request.user());
        } catch (ViewerRefusedException e) {
            viewer = true;
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                java.time.LocalDate.parse(gradeFrom);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date");
            }
        }
        body.set(new BulkGradeRequest(viewer || request.personIds() == null ? List.of() : request.personIds(), grade, gradeFrom, request.user()));
    }
}
