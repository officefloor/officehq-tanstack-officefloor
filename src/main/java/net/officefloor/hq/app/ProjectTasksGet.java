package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects/{projectId}/tasks — every task on one project's checklist, oldest id first.
 * Wired by {@code officefloor/rest/api/projects/{projectId}/tasks.GET.yml}.
 */
public class ProjectTasksGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            TaskRepository tasks, ObjectResponse<List<Task>> response) {
        response.send(tasks.findByProjectIdOrderByIdAsc(Long.valueOf(projectId)));
    }
}
