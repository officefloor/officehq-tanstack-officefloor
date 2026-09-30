import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectTagsPanel } from './ProjectTagsPanel';

// The tags' presence on a project's detail page — its own file, filling the project-detail slot. The
// detail route is not touched to add this; the panel queries for its own data (['tags'] and
// ['projectTags']) scoped to the project it is handed.
export const contribution = ProjectDetail.fill({
  order: 30,
  Component: ProjectTagsPanel,
});
