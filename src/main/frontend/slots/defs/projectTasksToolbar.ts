import { defineSlot } from '../Slot';

/**
 * The project-tasks toolbar — a region above a project's task checklist for controls that act on it
 * (filtering to just the open or just the finished tasks, and whatever comes later). ProjectTasksTable
 * renders the region; features fill it (CLAUDE.md rule 3), so a new control is a new `*.slot.tsx`
 * file and the table is not edited again to add one.
 */
export const ProjectTasksToolbar = defineSlot('project.tasks.toolbar');
