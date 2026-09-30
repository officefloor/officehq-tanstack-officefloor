import { defineSlot } from '../Slot';

/**
 * The per-row action region of the projects table. Each project row renders this with its own id;
 * features fill it (e.g. the link that opens the project) without the list ever listing the actions.
 */
export const ProjectRowActions = defineSlot<{ projectId: number }>('project.row.actions');
