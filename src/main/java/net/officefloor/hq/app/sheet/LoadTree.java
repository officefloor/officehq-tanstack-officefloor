package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.person.NameTree;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadTree {
    public void load(@Val Sheet sheet, NameTree names, Out<Tree> tree) {
        tree.set(new Tree(names.sheets(sheet.id())));
    }
}
