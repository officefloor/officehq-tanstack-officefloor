package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades per sheet — A, B and C, then those added in the order added — and those retired. A retired grade cannot be chosen — for a new person, in an edit, a bulk
 *  grade or an import — while the people already on it keep it: an edit leaving a person's grade as the one in
 *  force on the sheet's today, with no from-date, is not a choice of it (see {@link #refuseChoice}). */
@Repository
public class GradeStore {

    /** The grades every sheet has. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** A grade's code: one to three capital letters. */
    public static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[A-Z]{1,3}");

    private final JdbcTemplate jdbc;

    public GradeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades: A, B and C, then those added in the order added. Not kept by version: a grade added
     *  is one of the sheet's grades on every date, so the screen read as of a date lists it too. */
    public List<String> grades(String sheetId) {
        List<String> all = new java.util.ArrayList<>(GRADES);
        all.addAll(jdbc.queryForList("SELECT grade FROM grade_added WHERE sheet_id = ? ORDER BY id", String.class,
                sheetId));
        return all;
    }

    public boolean has(String sheetId, String grade) {
        return grade != null && grades(sheetId).contains(grade);
    }

    /** Refuses a grade the sheet does not have. */
    public void refuseUnknown(String sheetId, String grade) throws InvalidSheetException {
        if (!has(sheetId, grade)) {
            throw new InvalidSheetException("The grade must be one of " + String.join(", ", grades(sheetId)));
        }
    }

    public void add(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_added (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
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
