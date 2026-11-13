package net.officefloor.hq.app.alias;

import net.officefloor.plugin.variable.Val;

public class SaveAlias {
    public void save(@Val Alias alias, AliasRepository aliases) {
        aliases.save(alias);
    }
}
