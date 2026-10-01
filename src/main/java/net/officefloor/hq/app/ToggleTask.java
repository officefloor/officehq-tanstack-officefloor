package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/tasks/toggle — tick a task off (or back on): flip its done flag and return the updated
 * row. Wired by officefloor/rest/api/tasks/toggle.POST.yml.
 *
 * We reject a missing or unknown task id before writing anything so a bad request neither changes
 * state nor returns a row.
 */
public class ToggleTask {

    public void service(@RequestBody ToggleTaskForm form, TaskRepository tasks,
            ObjectResponse<TaskView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A task id is required");
        }
        Task task = tasks.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid task is required"));
        task.toggleDone();
        Task saved = tasks.save(task);
        response.send(TaskView.of(saved));
    }
}
