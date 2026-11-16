package net.officefloor.hq.app.grade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.person.InvalidPersonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GradeRepository {

    /** The grades a person may be on. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades, in order, each with whether it is retired. */
    public List<Grade> list(String sheetId) {
        Set<String> retired = retired(sheetId);
        return GRADES.stream().map((g) -> new Grade(g, retired.contains(g))).toList();
    }

    public Set<String> retired(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?",
                String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public GradeRetired retire(String sheetId, String grade, String by) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade, retired_by, retired_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                sheetId, grade, by);
        return new GradeRetired(sheetId, grade, by);
    }

    /** Refuses a grade chosen for a person (a new person, or an edit giving a grade) that is retired on the sheet. */
    public void requireChoosable(String sheetId, String grade) throws InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new InvalidPersonException("The grade is retired: " + grade);
        }
    }
}
