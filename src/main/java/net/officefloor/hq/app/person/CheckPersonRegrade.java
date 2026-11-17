package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses an edit choosing a grade retired on the sheet (see GradeRepository): one giving a from-date, or a grade other
 *  than the person's in force. An edit leaving the grade as it was, with no from-date, chooses nothing. */
public class CheckPersonRegrade {
    public void check(@PathVariable("sheetId") String sheetId, @Val Person person, @Val PersonValues values,
            GradeRepository grades) throws InvalidPersonException {
        refuse(sheetId.trim(), person, values, grades);
    }

    static void refuse(String sheet, Person person, PersonValues values, GradeRepository grades) throws InvalidPersonException {
        if (values.gradeFrom() != null || !values.grade().equals(person.grade())) {
            grades.checkChoosable(sheet, values.grade());
        }
    }
}
