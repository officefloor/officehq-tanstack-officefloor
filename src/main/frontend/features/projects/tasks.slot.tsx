import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectTasksPanel } from './ProjectTasksPanel';

// The tasks' presence on a project's detail page — its own file, filling the project-detail slot.
// The detail route was written once and is not touched to add this; the panel queries for its own
// data (['tasks']) scoped to the project it is handed.
export const contribution = ProjectDetail.fill({
  order: 20,
  Component: ProjectTasksPanel,
});
