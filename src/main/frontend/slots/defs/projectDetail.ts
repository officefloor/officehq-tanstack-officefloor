import { defineSlot } from '../Slot';

/**
 * A project's detail region. The project-detail route renders this once; features (its invoices,
 * and anything added later) fill it with `ProjectDetail.fill(...)`, each panel querying for itself.
 * The context is the project's id, so a contribution can scope its own data to that project.
 */
export const ProjectDetail = defineSlot<{ projectId: number }>('project.detail');
