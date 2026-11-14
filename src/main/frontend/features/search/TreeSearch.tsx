import { useState } from 'react';
import { getLiveJson } from '../../api/http';
import { useAliases } from '../aliases/aliases';
import type { Alias } from '../aliases/aliases';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { liveHolds, shownName, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDue, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { SheetInfo } from '../session/session';
import { dueRules, useDueRules } from '../settings/settings';
import type { DueRules, Settings } from '../settings/settings';

/** What a sheet holds that the search reads: its people (in list order), their aliases and what their due-totals need. */
type SheetData = { people: Person[]; aliases: Alias[]; claims: Claim[]; rates: Rate[]; rules: DueRules };

/** A sheet's data as the store holds it now, its due rules by its own today. */
async function sheetData(sheet: string): Promise<SheetData> {
  const path = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [info, people, aliases, claims, rates, settings] = await Promise.all([
    getLiveJson<SheetInfo>(path),
    getLiveJson<Person[]>(`${path}/people`),
    getLiveJson<Alias[]>(`${path}/aliases`),
    getLiveJson<Claim[]>(`${path}/claims`),
    getLiveJson<Rate[]>(`${path}/rates`),
    getLiveJson<Settings>(`${path}/settings`),
  ]);
  return { people, aliases, claims, rates, rules: dueRules(settings, info.today, claims) };
}

/** The lines `<sheet id> <name> (<grade>) <due-total>` for the sheet's people whose name or an alias contains the text, ignoring case. */
function matches(sheet: string, d: SheetData, text: string): string[] {
  const key = text.toLowerCase();
  const hit = (s: string) => s.toLowerCase().includes(key);
  return d.people
    .filter((p) => hit(p.name) || d.aliases.some((a) => a.personId === p.id && hit(a.alias)))
    .map((p) => {
      const due = personDue(d.claims.filter((c) => c.personId === p.id), d.rates, p.grades, liveHolds(p, d.rules.today), d.rules);
      return `${sheet} ${shownName(p)} ${due}`;
    });
}

// The tree search: every person on this sheet and the other sheets of its tree (its ancestors and descendants) whose
// name or an alias contains the typed text, ignoring case, one line each with their sheet, name and due-total as that
// sheet shows it — this sheet's people first in list order (retired people included), then the other sheets' by sheet id.
export function TreeSearch() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const aliases = useAliases().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const [text, setText] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');

  if (!user || !sheet) return null;

  const search = async () => {
    setLines(null);
    setError('');
    try {
      const ids = await getLiveJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`);
      const others = [...ids].sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
      const data = await Promise.all(others.map(sheetData));
      setLines([
        ...matches(sheet, { people, aliases, claims, rates, rules }, text),
        ...others.flatMap((id, i) => matches(id, data[i], text)),
      ]);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  return (
    <section>
      <input data-testid="search-name" value={text} placeholder="Search the tree" onChange={(e) => setText(e.target.value)} />
      <button type="button" data-testid="search" onClick={search}>
        Search
      </button>
      {lines && (
        <>
          {' '}Found: <strong data-testid="search-count">{lines.length}</strong>
          <ul>
            {lines.map((l, i) => (
              <li key={i} data-testid="search-line">
                {l}
              </li>
            ))}
          </ul>
        </>
      )}
      {error && <span data-testid="search-error">{error}</span>}
    </section>
  );
}
