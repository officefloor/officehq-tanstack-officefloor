import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectNotes } from './ProjectNotes';

// The notes panel's presence on the project detail page — its own file, filling the shared
// project.detail region. The detail route is not touched to add it (CLAUDE.md rule 3).
export const contribution = ProjectDetail.fill({ order: 30, Component: ProjectNotes });
