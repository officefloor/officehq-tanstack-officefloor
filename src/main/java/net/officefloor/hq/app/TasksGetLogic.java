package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/tasks?projectId=<id>} — list the tasks that belong to one project, oldest first,
 * so a project's detail page shows only its own checklist. Wired by
 * {@code officefloor/rest/api/tasks.GET.yml}.
 */
public class TasksGetLogic {

    public void service(@RequestParam("projectId") Long projectId, TaskRepository tasks,
            ObjectResponse<List<Task>> response) {
        response.send(tasks.findByProjectIdOrderByIdAsc(projectId));
    }
}
