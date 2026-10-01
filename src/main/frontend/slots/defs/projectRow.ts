import { defineSlot } from '../Slot';
import type { Project } from '../../features/projects/projects';

/**
 * The actions cell of one project row in the projects list — per-row controls that act on a single
 * project (deleting it, and more over time). The projects table renders this region once per row and
 * never lists what fills it; each action is its own `*.slot.tsx` file under `features/projects/`.
 * Handed the whole project as context, so an action has its id and name to work with.
 */
export const ProjectRow = defineSlot<{ project: Project }>('project.row');
