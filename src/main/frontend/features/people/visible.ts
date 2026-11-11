import { useCrews } from '../crews/crews';
import { useMyRole } from '../dashboard/dashboard';
import type { Person } from './people';

/**
 * Which people the signed-in user sees on the people list. A restricted user sees only the person with
 * their name and the people (leads and members) of the crews that person leads or belongs to; everyone
 * else sees everyone. Until the role, and for a restricted user the crews, are read, no one is shown.
 * Only the list is narrowed: every other figure stays over everyone on the sheet.
 */
export function useVisiblePeople(sheetId: string, user: string): (p: Person) => boolean {
  const role = useMyRole(sheetId, user);
  const crews = useCrews(sheetId);
  if (role.data === undefined) return () => false;
  if (role.data !== 'restricted') return () => true;
  if (crews.data === undefined) return () => false;
  const me = user.trim();
  const ids = new Set<string>();
  const names = new Set<string>([me]);
  for (const c of crews.data) {
    const inCrew = c.lead === me || c.members.some((m) => m.name === me);
    if (!inCrew) continue;
    if (c.leadId) ids.add(c.leadId);
    for (const m of c.members) ids.add(m.personId);
  }
  return (p) => ids.has(p.id) || names.has(p.name);
}
