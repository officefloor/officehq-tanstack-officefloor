import { defineSlot } from '../Slot';

/**
 * The projects toolbar: the strip above the projects table. The projects page renders it once;
 * features (e.g. the show-archived toggle) fill it with `ProjectsToolbar.fill(...)`. Adding a
 * control here is a new file, never an edit to the page.
 */
export const ProjectsToolbar = defineSlot('projects.toolbar');
