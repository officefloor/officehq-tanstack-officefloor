import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectTags } from './ProjectTags';

// The tags panel's presence on the project detail page — its own file, filling the shared
// project.detail region. The detail route is not touched to add it (CLAUDE.md rule 3). Ordered after
// the task list.
export const contribution = ProjectDetail.fill({ order: 20, Component: ProjectTags });
