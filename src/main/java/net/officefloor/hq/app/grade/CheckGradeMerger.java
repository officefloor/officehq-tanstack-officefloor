package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Only a steward may merge a grade. */
public class CheckGradeMerger {
    public void check(@PathVariable("sheetId") String sheetId, @Val MergeGradeValues values, RoleRepository roles)
            throws InvalidGradeException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheetId.trim(), values.by()))) {
            throw new InvalidGradeException("Only a steward may merge a grade");
        }
    }
}
