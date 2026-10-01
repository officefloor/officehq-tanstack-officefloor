import { defineSlot } from '../Slot';

/**
 * Controls that act on a project's task checklist — a filter today, more over time. Rendered above
 * the task table by the projects feature; each control is its own `*.slot.tsx` file and owns a URL
 * search param, so the list never lists its controls.
 */
export const ProjectTasksToolbar = defineSlot('projectTasks.toolbar');
