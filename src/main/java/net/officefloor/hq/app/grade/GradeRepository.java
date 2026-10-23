package net.officefloor.hq.app.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's grades (A, B and C, then those added to the sheet in the order added) and which of them are retired. */
@Repository
public class GradeRepository {

    /** The grades every sheet has, in order. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades, in order: A, B and C, then those added to it in the order added. */
    public List<String> grades(String sheetId) {
        List<String> all = new ArrayList<>(GRADES);
        all.addAll(jdbc.queryForList("SELECT grade FROM sheet_grade WHERE sheet_id = ? ORDER BY id", String.class,
                sheetId));
        return all;
    }

    /** Whether the grade is one of the sheet's grades. */
    public boolean isGrade(String sheetId, String grade) {
        return grade != null && grades(sheetId).contains(grade);
    }

    /** Add a grade to the sheet. */
    public void add(String sheetId, String grade, String by) {
        jdbc.update("INSERT INTO sheet_grade (sheet_id, grade, added_by) VALUES (?, ?, ?)", sheetId, grade, by);
    }

    /** The sheet's retired grades. */
    public Set<String> retired(String sheetId) {
        return Set.copyOf(jdbc.queryForList("SELECT grade FROM sheet_retired_grade WHERE sheet_id = ?", String.class,
                sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_retired_grade WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade, String by) {
        jdbc.update("INSERT INTO sheet_retired_grade (sheet_id, grade, retired_by) VALUES (?, ?, ?)", sheetId, grade,
                by);
    }

    /** Refuse a grade chosen for a person (added, edited or imported) when it is retired on the sheet. */
    public void refuseRetired(String sheetId, String grade) throws RetiredGradeException {
        if (grade != null && isRetired(sheetId, grade)) {
            throw new RetiredGradeException(grade);
        }
    }
}
