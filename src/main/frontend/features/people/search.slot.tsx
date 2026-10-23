import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import type { Claim } from '../claims/claims';
import { personDueTotal, type Rate } from '../rates/rates';
import { floorOf, lateOf, type Settings } from '../settings/settings';
import type { Sheet } from '../sheet/sheet';
import { nameWithGrade, type Person } from './people';

/** Whether the text (as typed, case ignored) is in the person's name or one of their aliases. */
const matches = (person: Person, text: string) =>
  [person.name, ...person.aliases].some((n) => n.toLowerCase().includes(text));

/** One sheet's matching people, in list order, each as <sheet id> <name> <due-total> (as shown on their row), read
 *  from the store as it is now. */
async function sheetLines(sheetId: string, text: string): Promise<string[]> {
  const read = <T,>(part: string) =>
    getJsonNow<T>(`/api/sheets/${encodeURIComponent(sheetId)}${part === '' ? '' : `/${part}`}`);
  const [people, claims, rates, settings, sheet] = await Promise.all([
    read<Person[]>('people'), read<Claim[]>('claims'), read<Rate[]>('rates'), read<Settings>('settings'), read<Sheet>(''),
  ]);
  const today = sheet.today;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, claims, people);
  return people
    .filter((p) => matches(p, text))
    .map((p) => `${sheetId} ${nameWithGrade(p, today)} ${personDueTotal(p, claims, rates, today, floor, late, settings.cap ?? '', people)}`);
}

/** A search box and button listing, over this sheet and every other sheet of its tree (its ancestors and its
 *  descendants), every person whose name or an alias contains the typed text (case ignored), retired people
 *  included: this sheet's people first in list order, then the other sheets' by sheet id. */
function TreeSearch({ sheetId }: { sheetId: string; user: string }) {
  const [text, setText] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');
  const latest = useRef(0);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const run = ++latest.current;
    setLines(null);
    setError('');
    const wanted = text.toLowerCase();
    try {
      const others = await getJsonNow<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
      const found = await Promise.all([sheetId, ...others].map((id) => sheetLines(id, wanted)));
      if (run === latest.current) setLines(found.flat());
    } catch {
      if (run === latest.current) setError('Could not search the tree.');
    }
  };

  return (
    <section>
      <form onSubmit={submit}>
        <label>
          Search the tree{' '}
          <input data-testid="search-name" value={text} onChange={(e) => setText(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="search">
          Search
        </button>{' '}
        found <strong data-testid="search-count">{lines === null ? '' : lines.length}</strong>
      </form>
      {error && <p data-testid="search-error">{error}</p>}
      <ul>
        {(lines ?? []).map((line, i) => (
          <li key={i} data-testid="search-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 10.5, Component: TreeSearch });
