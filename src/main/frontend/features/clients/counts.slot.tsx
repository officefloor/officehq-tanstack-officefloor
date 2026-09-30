import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientProjectsKey, fetchClientProjects } from './queries';
import { clientContactsKey, fetchClientContacts } from '../contacts/queries';

// At-a-glance counts of a client's projects and contacts — one new *.slot.tsx filling the
// ClientDetail region (CLAUDE.md rule 3). Nothing existing is edited. It reads the SAME nested keys
// the projects/contacts tables use (rule 5), so a write elsewhere that invalidates ['clients']
// refreshes these numbers too, with no import between features.
function ClientCounts({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: clientProjectsKey(clientId),
    queryFn: () => fetchClientProjects(clientId),
  });
  const { data: contacts } = useQuery({
    queryKey: clientContactsKey(clientId),
    queryFn: () => fetchClientContacts(clientId),
  });

  return (
    <dl data-testid="client-counts">
      <div>
        <dt>Jobs</dt>
        <dd data-testid="client-projects-count">{projects?.length ?? 0}</dd>
      </div>
      <div>
        <dt>Contacts</dt>
        <dd data-testid="client-contacts-count">{contacts?.length ?? 0}</dd>
      </div>
    </dl>
  );
}

export const contribution = ClientDetail.fill({ order: 5, Component: ClientCounts });
