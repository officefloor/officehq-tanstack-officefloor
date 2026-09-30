package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/tasks/toggle} — tick a task off or back on (flip its {@code done} flag) and return
 * the updated row. Wired by {@code officefloor/rest/api/tasks/toggle.POST.yml}. The task must exist;
 * the change is recorded through {@link Audit} so the tick can be checked back later (the UI shows
 * only the current state).
 */
public class TaskToggle {

    public void service(@RequestBody ToggleTask body, TaskRepository tasks, Audit audit,
            ObjectResponse<Task> response) {
        Long id = body.getId();
        Task task = id == null ? null : tasks.findById(id).orElse(null);
        if (task == null) {
            throw new IllegalArgumentException("no such task");
        }
        task.setDone(!task.isDone());
        Task saved = tasks.save(task);
        audit.record("TASK_TOGGLED id=" + saved.getId() + " done=" + saved.isDone());
        response.send(saved);
    }
}
