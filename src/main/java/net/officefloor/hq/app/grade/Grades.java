package net.officefloor.hq.app.grade;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the grades retired on each sheet. A retired grade cannot be chosen for a person; people on it keep it. */
@Repository
public class Grades {

    /** The grades a person may be on. */
    public static final List<String> ALL = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public Grades(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean retired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Each grade, in order, and whether it is retired on the sheet. */
    public List<GradeResponse> list(String sheetId) {
        List<String> retired = jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?", String.class, sheetId);
        return ALL.stream().map(g -> new GradeResponse(g, retired.contains(g))).toList();
    }

    /** The grade a new person is given when none is chosen: the sheet's default grade, or A when that is retired. */
    public String defaultFor(String sheetId, String defaultGrade) {
        return retired(sheetId, defaultGrade) ? "A" : defaultGrade;
    }
}
