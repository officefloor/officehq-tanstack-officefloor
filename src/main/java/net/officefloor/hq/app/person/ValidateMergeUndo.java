package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMergeUndo {
    public void validate(@RequestBody MergeUndoRequest request, Out<MergeUndoRequest> body, Out<Actor> actor) {
        MergeUndoRequest given = request != null ? request : new MergeUndoRequest(null);
        body.set(given);
        actor.set(new Actor(given.user()));
    }
}
