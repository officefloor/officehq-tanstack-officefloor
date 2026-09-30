import { defineSlot } from '../Slot';

/**
 * The controls above the projects list — where list-wide toggles and filters live (e.g. reveal
 * archived projects). Declared once here; features fill it without the table listing what goes in it
 * (CLAUDE.md rule 3). No context: the toolbar acts on the list as a whole.
 */
export const ProjectsToolbar = defineSlot('projects.toolbar');
