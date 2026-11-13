package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadWatches {
    public void load(@Val Sheet sheet, WatchRepository repository, Out<List<Watch>> watches) {
        watches.set(repository.findAll(sheet.id()));
    }
}
