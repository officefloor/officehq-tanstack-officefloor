package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadCrews {
    public void load(@Val Sheet sheet, CrewRepository repository, Out<Crews> crews) {
        crews.set(new Crews(repository.findAll(sheet.id())));
    }
}
