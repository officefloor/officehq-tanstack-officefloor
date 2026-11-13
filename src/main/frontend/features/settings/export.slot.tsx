import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { counts, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { claimDue, dueTotal, useRates } from '../rates/rates';
import { lateOf } from '../overdue/overdue';
import { floorOf, policyOf, useSettings } from './settings';

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads the policy versions as CSV: version, from-date (blank for none), the key and value it changed, how many
// of the sheet's claims have it as their policy version (as the policy viewer counts them) and the sum of the dues,
// as each claim row shows them, of those of these claims that count. One line per version, oldest first.
function ExportPolicy() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settingsQuery = useSettings(sheet);
  const settings = settingsQuery.data;

  if (!user || !sheet) return null;

  // The dues are read as the rows show them: wait until every one is loaded.
  const ready = !!people.data && !!claims.data && !!rates.data && !!settings && today !== undefined;

  const download = () => {
    const policy = policyOf(settings);
    const floor = floorOf(settings);
    const byId = new Map((people.data ?? []).map((p) => [p.id, p]));
    const numbers = new Map<number, number>();
    const dues = new Map<number, string[]>();
    for (const c of claims.data ?? []) {
      const version = policy?.(c.date) ?? 0;
      numbers.set(version, (numbers.get(version) ?? 0) + 1);
      const person = byId.get(c.personId);
      if (!counts(c) || !person) continue;
      const due = claimDue(c, rates.data, person.grades, floor, lateOf(settings, today, [person]));
      dues.set(version, [...(dues.get(version) ?? []), due]);
    }
    const froms = settings?.policyFrom ?? [];
    const lines = [['version', 'from', 'key', 'value', 'claims', 'due-total']];
    (settings?.policyChange ?? []).forEach((change, i) => {
      const at = change.indexOf('=');
      const [key, value] = at < 0 ? [change, ''] : [change.slice(0, at), change.slice(at + 1)];
      lines.push([String(i + 1), froms[i] ?? '', key, value, String(numbers.get(i + 1) ?? 0),
        dueTotal(dues.get(i + 1) ?? [])]);
    });
    const csv = lines.map((l) => l.map(cell).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `policy-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <p>
      <button type="button" data-testid="export-policy" disabled={!ready} onClick={download}>
        Export policy versions (CSV)
      </button>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 49.5, Component: ExportPolicy });
