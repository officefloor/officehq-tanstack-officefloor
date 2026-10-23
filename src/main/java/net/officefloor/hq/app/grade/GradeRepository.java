package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's grades (A, B and C) and which of them are retired. */
@Repository
public class GradeRepository {

    /** Every grade, in order. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
