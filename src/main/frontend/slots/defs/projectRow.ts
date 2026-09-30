import { defineSlot } from '../Slot';

/**
 * A cell at the end of every project row — the region where per-row actions live (e.g. "open this
 * project"). Declared once here; features fill it without the table listing what goes in it
 * (CLAUDE.md rule 3). Its context is the row's project id.
 */
export const ProjectRow = defineSlot<{ projectId: number }>('project.row');
