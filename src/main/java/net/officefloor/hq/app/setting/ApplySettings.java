package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class ApplySettings {
    public void apply(@Val SheetSettings settings, Settings store) {
        store.set(settings.sheetId(), Settings.CLERK_LIMIT, settings.clerkLimit());
    }
}
