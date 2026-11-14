package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.person.InvalidPersonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's grades (A, B, C) and which of them are retired. */
@Repository
public class GradeRepository {

    /** The grades a person may be on. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    /** Refuses a retired grade chosen for a person. */
    public void checkChoosable(String sheetId, String grade) throws InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new InvalidPersonException("The grade " + grade + " is retired.");
        }
    }

    /** The grade a new person takes when none is given: the default, unless that is retired (then A). */
    public String fallback(String sheetId, String defaultGrade) {
        return isRetired(sheetId, defaultGrade) ? "A" : defaultGrade;
    }
}
