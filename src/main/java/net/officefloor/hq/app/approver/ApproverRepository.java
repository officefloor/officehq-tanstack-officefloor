package net.officefloor.hq.app.approver;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each person's approver, per sheet: the user who may approve or reject their PENDING claims as a steward may. */
@Repository
public class ApproverRepository {

    private final JdbcTemplate jdbc;

    public ApproverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Make the user the person's approver, replacing any they had; a blank user leaves them without one. */
    public void set(String sheetId, long personId, String user) {
        if (user.isBlank()) {
            jdbc.update("DELETE FROM person_approver WHERE sheet_id = ? AND person_id = ?", sheetId, personId);
        } else {
            jdbc.update("MERGE INTO person_approver KEY (sheet_id, person_id) VALUES (?, ?, ?)", sheetId, personId,
                    user);
        }
    }

    /** The approvers on the sheet, by person id. */
    public List<ApproverResponse> all(String sheetId) {
        return jdbc.query("SELECT person_id, user_name FROM person_approver WHERE sheet_id = ? ORDER BY person_id",
                (rs, i) -> new ApproverResponse(rs.getLong("person_id"), rs.getString("user_name")), sheetId);
    }
}
