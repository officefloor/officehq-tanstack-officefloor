import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ClientDetail } from '../../slots/defs/clientDetail';
import type { Project } from '../projects/ProjectsPage';
import type { Contact } from '../contacts/ClientContactsPanel';

// At-a-glance counts of a client's projects and contacts — its own file, filling the client-detail
// slot above the panels (order 5, before projects at 10). It owns no data of its own: it reads the
// SAME ['projects'] and ['contacts'] keys the sibling panels own, scoped to the client it is handed,
// so a project or contact write that invalidates either key refreshes these counts too (rule 5).
// The counts are derived, never copied into state.
export function ClientCounts({ clientId }: { clientId: number }) {
  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: () => getJson<Project[]>('/api/projects'),
  });
  const contacts = useQuery({
    queryKey: ['contacts'],
    queryFn: () => getJson<Contact[]>('/api/contacts'),
  });

  const projectCount = (projects.data ?? []).filter((p) => p.clientId === clientId).length;
  const contactCount = (contacts.data ?? []).filter((c) => c.clientId === clientId).length;

  return (
    <dl data-testid="client-counts">
      <div>
        <dt>Jobs</dt>
        <dd data-testid="client-projects-count">{projectCount}</dd>
      </div>
      <div>
        <dt>Contacts</dt>
        <dd data-testid="client-contacts-count">{contactCount}</dd>
      </div>
    </dl>
  );
}

export const contribution = ClientDetail.fill({
  order: 5,
  Component: ClientCounts,
});
