package net.officefloor.hq.app.alias;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadAliases {
    public void load(@Val Sheet sheet, AliasRepository repository, Out<List<Alias>> aliases) {
        aliases.set(repository.findAll(sheet.id()));
    }
}
