package net.officefloor.hq.app.session;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A column of the people list, and whether to show it. */
public class ValidateColumn {
    public void validate(@PathVariable("column") String column, @RequestBody ColumnRequest request,
            Out<ColumnChoice> out) throws BadRequestException {
        String name = column == null ? "" : column.trim();
        if (!ColumnsRepository.COLUMNS.contains(name)) {
            throw new BadRequestException("No such column: " + name);
        }
        if (request == null || request.shown() == null) {
            throw new BadRequestException("Say whether to show the column");
        }
        out.set(new ColumnChoice(name, request.shown()));
    }
}
