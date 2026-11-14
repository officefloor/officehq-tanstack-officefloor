package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a change that went through is recorded. */
public class RecordRoleSet {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Role role, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + (by == null ? "" : by)
                + " user=" + role.user() + " role=" + role.role());
    }
}
