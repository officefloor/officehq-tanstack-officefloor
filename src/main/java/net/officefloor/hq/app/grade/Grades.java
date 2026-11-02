package net.officefloor.hq.app.grade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades of each sheet (A, B and C), and which of them are retired. */
@Repository
public class Grades {
    public static final List<String> ALL = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public Grades(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every grade with whether it is retired on the sheet, in order. */
    public List<GradeRow> list(String sheet) {
        Set<String> retired = retired(sheet);
        return ALL.stream().map((g) -> new GradeRow(g, retired.contains(g))).toList();
    }

    public Set<String> retired(String sheet) {
        return new HashSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet = ?", String.class, sheet));
    }

    public boolean isRetired(String sheet, String grade) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet = ? AND grade = ?",
                Integer.class, sheet, grade);
        return found != null && found > 0;
    }

    public void retire(String sheet, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet, grade) VALUES (?, ?)", sheet, grade);
    }
}
