import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, usePeople } from './people';
import type { Person } from './people';
import { setPersonError } from './personError';
import { useSelectedPeople } from './selection';

// Gives every ticked person a grade from a date (blank: the sheet's today), one after another in list
// order. Each goes through the person's own edit, exactly as the person form saves it (keeping their
// name, terms and contact, from the version listed), so it is audited and rolled up the same way. One the server
// refuses is left as it is and shows the refusal on its row; the count is of those changed.
export function BulkGrade() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [selected, , clear] = useSelectedPeople();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [from, setFrom] = useState('');
  const [graded, setGraded] = useState<number | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setGraded(undefined);
    setBusy(true);
    const ordered = people.filter((p) => selected.has(p.id));
    let changed = 0;
    for (const p of ordered) setPersonError(sheet, p.id, '');
    try {
      for (const p of ordered) {
        try {
          await putJson<Person>(
            `/api/sheets/${encodeURIComponent(sheet)}/people/${p.id}?user=${encodeURIComponent(user)}`,
            { name: p.name, grade, gradeFrom: from, terms: p.terms == null ? '' : String(p.terms),
              contact: p.contact ?? '', tags: p.tags.join(','), version: p.version },
          );
          changed++;
        } catch {
          setPersonError(sheet, p.id, 'That person could not be given the grade.');
        }
      }
      clear();
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    } finally {
      setBusy(false);
      setGraded(changed);
    }
  };

  return (
    <p>
      <select data-testid="bulk-grade" value={grade} onChange={(e) => setGrade(e.target.value)}>
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
      <input
        data-testid="bulk-grade-from"
        type="date"
        title="Grade from (blank: the sheet's today)"
        value={from}
        onChange={(e) => setFrom(e.target.value)}
      />{' '}
      <button type="button" data-testid="bulk-grade-apply" disabled={busy} onClick={() => once(run)}>
        Grade selected people
      </button>
      {graded !== undefined && (
        <>
          {' '}
          Changed <span data-testid="bulk-graded">{graded}</span>
        </>
      )}
    </p>
  );
}
