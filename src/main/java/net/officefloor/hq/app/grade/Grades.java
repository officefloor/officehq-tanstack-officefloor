package net.officefloor.hq.app.grade;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the grades of each sheet (A, B and C, then those added on it) and of those retired on it. A retired grade
 *  cannot be chosen for a person; people on it keep it. */
@Repository
public class Grades {

    /** The grades every sheet has. */
    public static final List<String> ALL = List.of("A", "B", "C");

    /** The form of a grade's code: one to three capital letters. */
    public static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[A-Z]{1,3}");

    private final JdbcTemplate jdbc;

    public Grades(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades, in order: A, B and C, then those added on it, in the order they were added. */
    public List<String> all(String sheetId) {
        List<String> all = new java.util.ArrayList<>(ALL);
        all.addAll(jdbc.queryForList("SELECT grade FROM grade WHERE sheet_id = ? ORDER BY seq", String.class, sheetId));
        return List.copyOf(all);
    }

    /** Whether the grade is one of the sheet's. */
    public boolean exists(String sheetId, String grade) {
        return all(sheetId).contains(grade);
    }

    public void add(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
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
        return all(sheetId).stream().map(g -> new GradeResponse(g, retired.contains(g))).toList();
    }

    /** The grade a new person is given when none is chosen: the sheet's default grade, or A when that is retired. */
    public String defaultFor(String sheetId, String defaultGrade) {
        return retired(sheetId, defaultGrade) ? "A" : defaultGrade;
    }
}
