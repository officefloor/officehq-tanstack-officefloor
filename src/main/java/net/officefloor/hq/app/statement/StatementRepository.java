package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The statements taken of each person, in the order they were taken (by id). */
@Repository
public class StatementRepository {

    private final JdbcTemplate jdbc;

    public StatementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String sheetId, long personId, BigDecimal dueTotal, LocalDate takenOn) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, due_total, taken_on) VALUES (?, ?, ?, ?)", sheetId,
                personId, dueTotal, Date.valueOf(takenOn));
    }
}
