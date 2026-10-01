package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/tasks/toggle} — tick a task off (or back on) from an {id} body and return the
 * updated row. Wired by {@code officefloor/rest/api/tasks/toggle.POST.yml}. Flips the task's done
 * flag (OPEN &lt;-&gt; DONE) and appends one {@code TASK_TOGGLED} audit record, so the change can be
 * checked back later through the audit file even though the UI only shows the status.
 */
public class TasksToggleLogic {

    public void service(@RequestBody ToggleTask body, TaskRepository tasks, Audit audit,
            ObjectResponse<Task> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A task id is required");
        }
        Task task = tasks.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such task"));
        task.setDone(!task.isDone());
        Task saved = tasks.save(task);
        audit.record("TASK_TOGGLED id=" + saved.getId() + " done=" + saved.isDone());
        response.send(saved);
    }

    /** Request body for toggling a task's done flag. */
    public static class ToggleTask {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
