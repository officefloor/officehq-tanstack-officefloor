package net.officefloor.hq.app.grade;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The retired grade store: the grades retired on each sheet. A retired grade cannot be chosen. */
@Repository
public class RetiredGrades {

    /** The grades a person can be on. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public RetiredGrades(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Whether the grade is retired on the sheet. */
    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM retired_grade WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    /** Retires the grade on the sheet. */
    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO retired_grade (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Every grade, in order, with whether it is retired on the sheet. */
    public List<GradeResponse> list(String sheetId) {
        List<String> retired = jdbc.queryForList("SELECT grade FROM retired_grade WHERE sheet_id = ?", String.class, sheetId);
        return GRADES.stream().map(g -> new GradeResponse(g, retired.contains(g))).toList();
    }

    /** Refuses choosing a retired grade. */
    public void refuseRetired(String sheetId, String grade) throws net.officefloor.hq.app.person.InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new net.officefloor.hq.app.person.InvalidPersonException("Grade " + grade + " is retired");
        }
    }
}
