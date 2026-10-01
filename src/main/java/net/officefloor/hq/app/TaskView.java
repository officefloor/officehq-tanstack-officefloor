package net.officefloor.hq.app;

/**
 * What the API exposes for a task: the shape the front-end renders into the project's tasks table.
 * {@code done} is the raw tick-off flag; the UI maps it to the OPEN/DONE label it shows.
 */
public record TaskView(Long id, Long projectId, String title, boolean done) {

    public static TaskView of(Task task) {
        return new TaskView(task.getId(), task.getProjectId(), task.getTitle(), task.isDone());
    }
}
