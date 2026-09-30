package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/tasks} — add a task (title) to a project and return the created row, OPEN by
 * default. Wired by {@code officefloor/rest/api/tasks.POST.yml}. The project must exist; the create
 * is audited through {@link Audit}.
 */
public class TasksPost {

    public void service(@RequestBody NewTask body, TaskRepository tasks, ProjectRepository projects,
            Audit audit, ObjectResponse<Task> response) {
        String title = body.getTitle() == null ? "" : body.getTitle().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("a task requires a title");
        }
        Long projectId = body.getProjectId();
        Project project = projectId == null ? null : projects.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("a task requires an existing project");
        }
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDone(false);
        Task saved = tasks.save(task);
        audit.record("TASK_CREATED id=" + saved.getId() + " title=" + saved.getTitle()
                + " project=" + project.getName());
        response.send(saved);
    }
}
