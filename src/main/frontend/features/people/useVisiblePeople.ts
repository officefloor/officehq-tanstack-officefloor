import { useCrews } from '../crews/useCrews';
import { roleOf, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import type { Person } from './usePeople';

/** Which people a user sees on the people list: everyone, except for a restricted user, who sees only the
 *  person with their name (compared trimmed, ignoring case) and the leads and members of every crew that
 *  person leads or belongs to (no one when there is no such person). Undefined while the role is not yet
 *  known, so no row shows before it is. Only the list's rows: every other figure is still over everyone. */
export function useVisiblePeople(sheetId: string, user: string, people: Person[] | undefined): Set<number> | 'all' | undefined {
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const crews = useCrews(sheetId).data;
  if (settings === undefined || today === undefined) return undefined;
  if (roleOf(settings, user, today) !== 'restricted') return 'all';
  if (people === undefined || crews === undefined) return undefined;
  const key = user.trim().toLowerCase();
  const self = people.find((p) => p.name.trim().toLowerCase() === key);
  const shown = new Set<number>();
  if (self === undefined) return shown;
  shown.add(self.id);
  for (const crew of crews) {
    if (crew.leadId !== self.id && !crew.members.some((m) => m.personId === self.id)) continue;
    shown.add(crew.leadId);
    for (const m of crew.members) shown.add(m.personId);
  }
  return shown;
}
