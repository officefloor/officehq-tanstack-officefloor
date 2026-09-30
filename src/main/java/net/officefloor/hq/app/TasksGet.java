package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/tasks} — list every task, oldest first. The UI scopes them to one project. Wired
 * by {@code officefloor/rest/api/tasks.GET.yml}.
 */
public class TasksGet {

    public void service(TaskRepository tasks, ObjectResponse<List<Task>> response) {
        response.send(tasks.findAllByOrderByIdAsc());
    }
}
