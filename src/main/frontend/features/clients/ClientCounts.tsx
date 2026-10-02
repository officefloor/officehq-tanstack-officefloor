import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useContacts } from './contacts';

// At-a-glance counts for one client: how many projects and contacts they have. Both are derived
// from the SHARED query keys (CLAUDE.md rule 5) — ['projects'] and ['contacts'] — the same data the
// detail panels already show, filtered to this client. Nothing is copied into state and no parent
// hands these down; creating a project or contact anywhere invalidates the key and the counts
// refresh themselves.
type Project = { id: number; clientId: number };

export function ClientCounts({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: ['projects'] as const,
    queryFn: () => getJson<Project[]>('/api/projects'),
  });
  const { data: contacts } = useContacts();

  const projectCount = (projects ?? []).filter((p) => p.clientId === clientId).length;
  const contactCount = (contacts ?? []).filter((c) => c.clientId === clientId).length;

  return (
    <dl data-testid="client-counts">
      <dt>Jobs</dt>
      <dd data-testid="client-projects-count">{projectCount}</dd>
      <dt>Contacts</dt>
      <dd data-testid="client-contacts-count">{contactCount}</dd>
    </dl>
  );
}
