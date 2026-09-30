import { ProjectDetail } from '../../slots/defs/projectDetail';
import { InvoicesPanel } from './InvoicesPanel';

// The invoices' presence on a project's detail page — its own file, filling the project-detail
// slot. The detail route was written once and is not touched to add this; the panel queries for its
// own data (['invoices']) scoped to the project it is handed.
export const contribution = ProjectDetail.fill({
  order: 10,
  Component: InvoicesPanel,
});
