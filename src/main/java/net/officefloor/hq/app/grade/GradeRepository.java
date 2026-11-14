package net.officefloor.hq.app.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.InvalidPersonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's grades (A, B, C, then those added on it, in the order added) and which of them are retired. */
@Repository
public class GradeRepository {

    /** The grades every sheet has. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** The form of a grade's code: one to three capital letters. */
    public static final Pattern CODE = Pattern.compile("[A-Z]{1,3}");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades: A, B, C, then those added, in the order added. */
    public List<String> grades(String sheetId) {
        List<String> all = new ArrayList<>(GRADES);
        all.addAll(jdbc.queryForList("SELECT grade FROM grade_added WHERE sheet_id = ? ORDER BY position", String.class,
                sheetId));
        return all;
    }

    public boolean exists(String sheetId, String grade) {
        if (GRADES.contains(grade)) {
            return true;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_added WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void add(String sheetId, String grade) {
        Integer last = jdbc.queryForObject("SELECT COALESCE(MAX(position), 0) FROM grade_added WHERE sheet_id = ?",
                Integer.class, sheetId);
        jdbc.update("INSERT INTO grade_added (sheet_id, grade, position) VALUES (?, ?, ?)", sheetId, grade,
                (last == null ? 0 : last) + 1);
    }

    /** The sheet's retired grades. */
    public Set<String> retired(String sheetId) {
        return new TreeSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?", String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Refuses a grade chosen for a person that is not one of the sheet's, or is retired. */
    public void checkChoosable(String sheetId, String grade) throws InvalidPersonException {
        if (!exists(sheetId, grade)) {
            throw new InvalidPersonException("The grade " + grade + " is not one of the sheet's grades.");
        }
        if (isRetired(sheetId, grade)) {
            throw new InvalidPersonException("The grade " + grade + " is retired.");
        }
    }

    /** The grade a new person takes when none is given: the default, unless that is retired (then A). */
    public String fallback(String sheetId, String defaultGrade) {
        return isRetired(sheetId, defaultGrade) ? "A" : defaultGrade;
    }
}
