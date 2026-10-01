import { defineSlot } from '../Slot';

/**
 * Controls that act on the projects list — a filter, a toggle, a sort. Rendered above the projects
 * table by the projects feature; each control is its own `*.slot.tsx` file and owns a URL search
 * param, so the list never lists its controls. No context: it spans every project.
 */
export const ProjectsToolbar = defineSlot('projects.toolbar');
