package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/tasks?projectId=&lt;id&gt; — the tasks of one project. Scoped to a project (the detail page
 * lists ITS tasks), so the project id arrives as a query parameter; rows come back in id order.
 * Wired by officefloor/rest/api/tasks.GET.yml.
 */
public class ListTasks {

    public void service(@RequestParam("projectId") String projectId,
            TaskRepository tasks, ObjectResponse<List<TaskView>> response) {
        Long id = Long.valueOf(projectId);
        List<TaskView> view = tasks.findByProjectIdOrderByIdAsc(id).stream()
                .map(TaskView::of).toList();
        response.send(view);
    }
}
