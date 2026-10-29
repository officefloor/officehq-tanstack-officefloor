import { useQuery } from '@tanstack/react-query';
import { crewsQuery } from '../crews/crewsQuery';
import { rolesQuery } from '../settings/settingsQuery';
import { useUser } from '../session/useUser';
import type { Person } from './peopleQuery';

const nameKey = (name: string) => name.trim().toLowerCase();

/**
 * Whether a person has a row on the people list for the signed-in user. A restricted user sees only the person whose
 * name or alias is their name (as names are compared: trimmed, ignoring case) and the people of the crews that person
 * leads or belongs to; with no such person, no one. Every other role sees everyone. Only the rows are narrowed: every
 * figure is still over everyone on the sheet.
 */
export function useListedFor(sheetId: string, people: Person[]): (person: Person) => boolean {
  const [user] = useUser();
  const roles = useQuery(rolesQuery(sheetId)).data;
  const crews = useQuery(crewsQuery(sheetId)).data ?? [];
  const name = user.trim();
  const restricted = roles?.some((r) => r.user === name && r.role === 'restricted') ?? false;
  if (!restricted) return () => true;
  const me = nameKey(name);
  const mine = me ? people.find((p) => [p.name, ...p.aliases].some((n) => nameKey(n) === me)) : undefined;
  if (!mine) return () => false;
  const seen = new Set<number>([mine.id]);
  for (const c of crews) {
    if (c.leadId === mine.id || c.memberIds.includes(mine.id)) {
      seen.add(c.leadId);
      c.memberIds.forEach((id) => seen.add(id));
    }
  }
  return (p) => seen.has(p.id);
}
