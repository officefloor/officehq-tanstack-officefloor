package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades retired per sheet. A retired grade cannot be chosen — for a new person, in an edit, a bulk
 *  grade or an import — while the people already on it keep it: an edit leaving a person's grade as the one in
 *  force on the sheet's today, with no from-date, is not a choice of it (see {@link #refuseChoice}). */
@Repository
public class GradeStore {

    /** The grades a sheet has. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's retired grades. */
    public List<String> retired(String sheetId) {
        return jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ? ORDER BY grade", String.class,
                sheetId);
    }

    /** The sheet's retired grades at the end of the date (see {@link History}). */
    public List<String> retiredAsOf(String sheetId, LocalDate date) {
        return jdbc.queryForList("SELECT grade FROM " + History.asOf("grade_retired", "id", date)
                + " WHERE sheet_id = ? ORDER BY grade", String.class, sheetId);
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Refuses choosing a retired grade for a new person (person null), or for a person in an edit — unless
     *  the edit leaves their grade as the one in force on the sheet's today with no from-date. */
    public void refuseChoice(String sheetId, String grade, Person person, String gradeFrom, LocalDate today)
            throws InvalidSheetException {
        if (grade == null || !isRetired(sheetId, grade)) {
            return;
        }
        if (person != null && gradeFrom == null && grade.equals(person.gradeOn(today))) {
            return;
        }
        throw new InvalidSheetException("The grade " + grade + " is retired");
    }
}
