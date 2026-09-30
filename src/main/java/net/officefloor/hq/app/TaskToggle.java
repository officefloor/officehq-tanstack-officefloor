package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/tasks/{id}/toggle — tick a task off (or back on) and return the updated row. Wired by
 * {@code officefloor/rest/api/tasks/{id}/toggle.POST.yml}. Flipping the done flag is an audited
 * side-effect: one {@code TASK_DONE id=<id>} / {@code TASK_REOPENED id=<id>} record is appended per
 * toggle so it can be checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}).
 * An unknown task id is rejected with 404.
 */
public class TaskToggle {

    public void service(@HttpPathParameter("id") String id, TaskRepository tasks, Audit audit,
            ObjectResponse<Task> response) {
        Task task = tasks.findById(Long.valueOf(id))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        task.setDone(!task.isDone());
        Task saved = tasks.save(task);
        audit.record((saved.isDone() ? "TASK_DONE" : "TASK_REOPENED") + " id=" + saved.getId());
        response.send(saved);
    }
}
