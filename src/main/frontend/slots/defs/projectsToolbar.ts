import { defineSlot } from '../Slot';

/**
 * The toolbar above the projects list. Features fill this region with controls that narrow or widen
 * the one projects list — the show-archived toggle, and whatever controls come later. The projects
 * list renders it; it never lists what goes in it, so each control is a new *.slot.tsx file that
 * owns its own URL key.
 */
export const ProjectsToolbar = defineSlot('projects.toolbar');
