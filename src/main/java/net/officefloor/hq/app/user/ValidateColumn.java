package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The column must be one the people list has, and the body must say whether it is shown. */
public class ValidateColumn {
    public void validate(@PathVariable("column") String column, @RequestBody ColumnRequest request,
            Out<ColumnRequest> body) throws InvalidColumnException {
        if (!ColumnRepository.COLUMNS.contains(column.trim()) || request == null || request.shown() == null) {
            throw new InvalidColumnException("Choose a column of the people list, shown or not.");
        }
        body.set(request);
    }
}
