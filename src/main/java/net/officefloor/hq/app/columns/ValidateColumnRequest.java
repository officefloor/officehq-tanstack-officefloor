package net.officefloor.hq.app.columns;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a column the people list does not let a user switch off, or no shown flag. */
public class ValidateColumnRequest {

    static final Set<String> COLUMNS = Set.of("claimed", "due-total", "excess", "statement-count", "stated-total",
            "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold", "retired", "terms",
            "contact", "aliases");

    public void validate(@RequestParam("user") String user, @RequestBody ColumnRequest request,
            Out<ColumnChoice> choice) throws InvalidColumnException {
        String column = request.column() == null ? "" : request.column().trim();
        if (!COLUMNS.contains(column) || request.shown() == null) {
            throw new InvalidColumnException("No such column: " + column);
        }
        choice.set(new ColumnChoice(user, column, request.shown()));
    }
}
