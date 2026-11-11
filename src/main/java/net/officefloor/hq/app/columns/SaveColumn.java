package net.officefloor.hq.app.columns;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Val;

/** The signed-in user's column choice is saved; it moves no figures and writes no audit line. */
public class SaveColumn {
    public void save(@Val ColumnInput input, ColumnsRepository columns, HttpServletRequest request)
            throws InvalidSettingException {
        String user = Audit.user(request);
        if (user.isBlank()) throw new InvalidSettingException("Sign in to choose columns");
        columns.save(user, input.column(), input.shown());
    }
}
