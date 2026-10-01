import { defineSlot } from '../Slot';

/**
 * A project's detail page. Features fill this region with panels about one project (its invoices,
 * their total, an add-invoice form). The detail route renders it with the project id as context;
 * the page never lists what goes in it, so each panel is a new *.slot.tsx file.
 */
export const ProjectDetail = defineSlot<{ projectId: number }>('project.detail');
