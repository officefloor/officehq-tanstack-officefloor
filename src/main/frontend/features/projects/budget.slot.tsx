import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectBudget } from './ProjectBudget';

// The budget panel's presence on the project detail page — its own file, filling the shared
// project.detail region (CLAUDE.md rule 3). The detail route is not touched to add it.
export const contribution = ProjectDetail.fill({ order: 5, Component: ProjectBudget });
