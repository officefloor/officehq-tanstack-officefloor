package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class ApplyRole {
    public void apply(@Val Role role, Settings settings) {
        settings.save(role);
    }
}
