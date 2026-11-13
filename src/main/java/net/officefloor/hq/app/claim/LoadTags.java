package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadTags {
    public void load(@Val Sheet sheet, ClaimRepository repository, Out<Tags> tags) {
        tags.set(new Tags(repository.usedTags(sheet.id())));
    }
}
