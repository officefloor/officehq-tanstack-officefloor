package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadPatrons {
    public void load(@Val Sheet sheet, PatronRepository repository, Out<List<Patron>> patrons) {
        patrons.set(repository.findAll(sheet.id()));
    }
}
