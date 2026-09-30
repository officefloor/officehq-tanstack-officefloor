import { defineSlot } from '../Slot';

/**
 * The body of a project's detail page — the region where per-project panels live (the task
 * checklist, and anything added later). Declared once here; features fill it without the page
 * listing what goes in it (CLAUDE.md rule 3). Its context is the project's id.
 */
export const ProjectDetail = defineSlot<{ projectId: number }>('project.detail');
