package net.officefloor.hq.app.columns;

import java.util.Set;
import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateColumn {

    /** The people-list columns a user may switch off. */
    static final Set<String> COLUMNS = Set.of("claimed", "due-total", "excess", "statement-count", "stated-total",
            "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold", "retired", "terms",
            "contact", "aliases");

    public void validate(@RequestBody ColumnRequest request, Out<ColumnInput> body) throws InvalidSettingException {
        String column = request == null || request.column() == null ? "" : request.column().trim();
        if (!COLUMNS.contains(column)) throw new InvalidSettingException("No such column");
        if (request.shown() == null) throw new InvalidSettingException("A column is shown or not");
        body.set(new ColumnInput(column, request.shown()));
    }
}
