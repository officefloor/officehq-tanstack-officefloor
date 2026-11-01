package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades of each sheet (A, B and C) and which of them are retired: a retired grade cannot be
 *  chosen for a person, though the people already on it keep it. */
@Repository
public class Grades {

    /** Every grade, in order. */
    public static final List<String> ALL = List.of("A", "B", "C");

    /** The grade a choice falls back to when the one it would take is retired. */
    public static final String FALLBACK = "A";

    private final JdbcTemplate jdbc;

    public Grades(JdbcTemplate jdbc) {
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

    /** The grade, or the fallback when it is retired on the sheet. */
    public String orFallback(String sheetId, String grade) {
        return grade != null && isRetired(sheetId, grade) ? FALLBACK : grade;
    }

    /** Refused when the grade is retired on the sheet. */
    public void refuseRetired(String sheetId, String grade) throws InvalidSheetRequestException {
        if (grade != null && isRetired(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade " + grade + " is retired");
        }
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }
}
