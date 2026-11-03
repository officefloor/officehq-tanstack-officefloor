package net.officefloor.hq.app.columns;

import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateColumns {
    public void validate(@RequestBody ColumnsRequest request, Out<UserColumns> columns) throws BadRequestException {
        List<String> raw = request == null || request.hidden() == null ? List.of() : request.hidden();
        for (String column : raw) {
            if (!ColumnsRepository.COLUMNS.contains(column)) {
                throw new BadRequestException("unknown column: " + column);
            }
        }
        // Kept in the list's own order, each once.
        columns.set(new UserColumns(ColumnsRepository.COLUMNS.stream().filter(raw::contains).toList()));
    }
}
