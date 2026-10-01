import { defineSlot } from '../Slot';

/**
 * A row action on the projects list — a per-project control rendered in the row's last cell (the
 * Slot is a Fragment, so it is valid inside a <td>). Features add actions here as new *.slot.tsx
 * files; the list never lists them. First use: the link that opens a project's detail page.
 */
export const ProjectRow = defineSlot<{ projectId: number }>('project.row');
