package net.officefloor.hq.app.grade;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The retired grade store: the grades retired on each sheet. A retired grade cannot be chosen. */
@Repository
public class RetiredGrades {

    /** The standing grades every sheet has; a sheet may add more (see {@link #add}). */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** A grade's code: one to three capital letters. */
    public static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[A-Z]{1,3}");

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

    /** The sheet's grades: the standing ones, then those added to it in the order added. */
    public List<String> codes(String sheetId) {
        List<String> codes = new java.util.ArrayList<>(GRADES);
        codes.addAll(jdbc.queryForList("SELECT grade FROM sheet_grade WHERE sheet_id = ? ORDER BY seq", String.class, sheetId));
        return codes;
    }

    /** Whether the code is one of the sheet's grades. */
    public boolean isGrade(String sheetId, String grade) {
        return grade != null && codes(sheetId).contains(grade);
    }

    /** Adds a grade to the sheet. */
    public void add(String sheetId, String grade) {
        jdbc.update("INSERT INTO sheet_grade (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Retires the grade on the sheet. */
    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO retired_grade (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** Every grade, in order, with whether it is retired on the sheet. */
    public List<GradeResponse> list(String sheetId) {
        List<String> retired = jdbc.queryForList("SELECT grade FROM retired_grade WHERE sheet_id = ?", String.class, sheetId);
        return codes(sheetId).stream().map(g -> new GradeResponse(g, retired.contains(g))).toList();
    }

    /** Refuses choosing a retired grade. */
    public void refuseRetired(String sheetId, String grade) throws net.officefloor.hq.app.person.InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new net.officefloor.hq.app.person.InvalidPersonException("Grade " + grade + " is retired");
        }
    }
}
