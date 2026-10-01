import { defineSlot } from '../Slot';

/**
 * A project's detail page — panels about one project (its task list, and more over time). The detail
 * route renders this region and never lists what fills it; each panel is its own `*.slot.tsx` file
 * under `features/projects/`. Handed the project id as context.
 */
export const ProjectDetail = defineSlot<{ projectId: number }>('project.detail');
