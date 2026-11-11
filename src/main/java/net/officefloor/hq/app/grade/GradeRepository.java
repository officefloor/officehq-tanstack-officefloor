package net.officefloor.hq.app.grade;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades retired on each sheet. */
@Repository
public class GradeRepository {

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The grades retired on the sheet. */
    public Set<String> retired(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?", String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** The grade a new person takes when none is given: the default grade, else (or when it is retired) A. */
    public String newPersonGrade(Settings settings) {
        String grade = settings.newPersonGrade();
        return isRetired(settings.sheetId(), grade) ? "A" : grade;
    }
}
