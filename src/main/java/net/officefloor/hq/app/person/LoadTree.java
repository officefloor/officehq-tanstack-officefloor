package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadTree {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PersonRepository repository, Out<Tree> tree) throws BadRequestException {
        CurrentUser.require(connection);
        List<String> sheets = new java.util.ArrayList<>(repository.tree(sheetId == null ? "" : sheetId.trim()));
        java.util.Collections.sort(sheets);
        tree.set(new Tree(sheets));
    }
}
