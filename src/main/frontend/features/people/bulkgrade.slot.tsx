import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { GRADES, peoplePath, peopleQueryKey, useSelectedPeople, useShowRetired, type Person } from './people';

// Gives every selected person a grade from a date (blank: the sheet's today) with one button, in list order. Each
// is saved exactly as the person form would save them (same name, terms, contact and tags, the version as the store holds it now),
// so the server writes the same PERSON_EDITED and rollup lines; one it refuses is left as it is. The count of
// people changed shows once all are done, and is gone while it runs; the selection is cleared.

function SelectHeader() {
  return <th>Select</th>;
}

function PersonSelect({ person }: { person: Person }) {
  const { selected, toggle } = useSelectedPeople();
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selected.has(person.id)}
        onChange={(e) => toggle(person.id, e.target.checked)}
      />
    </td>
  );
}

function BulkGrade() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedPeople();
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState('A');
  const [from, setFrom] = useState('');
  const [result, setResult] = useState<{ graded: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const run = async () => {
    if (busy.current) return;
    busy.current = true;
    setResult(null);
    try {
      let graded = 0;
      let refused = 0;
      try {
        const people = await queryClient.fetchQuery({
          queryKey: peopleQueryKey(sheet),
          queryFn: () => getJson<Person[]>(peoplePath(sheet)),
          staleTime: 0,
        });
        const order = people.filter((p) => (showRetired || p.retiredFrom === null) && selected.has(p.id));
        for (const p of order) {
          try {
            await putJson<Person>(`${peoplePath(sheet)}/${p.id}`, {
              name: p.name,
              grade,
              gradeFrom: from.trim(),
              terms: p.terms == null ? '' : String(p.terms),
              contact: p.contact ?? '',
              tags: (p.tags ?? []).join(', '),
              version: p.version,
            }, user);
            graded++;
          } catch {
            refused++;
          }
        }
      } catch {
        refused++;
      }
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
      clear();
      setResult({ graded, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      <select data-testid="bulk-grade" value={grade} onChange={(e) => { setGrade(e.target.value); setResult(null); }}>
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <input
        data-testid="bulk-grade-from"
        placeholder="Grade from (YYYY-MM-DD)"
        value={from}
        onChange={(e) => { setFrom(e.target.value); setResult(null); }}
      />{' '}
      <button type="button" data-testid="bulk-grade-apply" onClick={() => void run()}>
        Grade selected people
      </button>
      {result && (
        <>
          {' '}Changed <span data-testid="bulk-graded">{result.graded}</span>
          {result.graded === 0 && result.refused > 0 && (
            <>{' '}<span data-testid="person-error">The grade could not be given.</span></>
          )}
        </>
      )}
    </div>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 1, Component: SelectHeader }),
  PersonCells.fill({ order: 1, Component: PersonSelect }),
  HomeMain.fill({ order: 20.5, Component: BulkGrade }),
];
