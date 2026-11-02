package net.officefloor.hq.app.columns;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateColumnChoice {
    public void validate(@RequestBody ColumnChoice request, Out<ColumnChoice> body) throws InvalidColumnException {
        String column = request == null || request.column() == null ? "" : request.column().trim();
        if (!Columns.ALL.contains(column) || request.shown() == null) {
            throw new InvalidColumnException("No such column");
        }
        body.set(new ColumnChoice(column, request.shown()));
    }
}
