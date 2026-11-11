import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import type { Grade } from '../people/people';
import { clearRowError } from '../people/rowError';
import { useUser } from '../session/session';
import { clearSelectedPeople, selectPerson, useSelectedPeople } from './selection';
import { useGradeCodes } from '../grades/grades';

function SelectHead() {
  return <th>Select</th>;
}

// A tick on each person's row, picking them for the bulk grade.
function PersonSelect({ sheetId, person }: { sheetId: string; person: Person }) {
  const selected = useSelectedPeople(sheetId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selected.has(person.id)}
        onChange={(e) => selectPerson(sheetId, person.id, e.target.checked)}
      />
    </td>
  );
}

// Gives the ticked people a grade together, from a from-date ('' for the sheet's today). The server
// grades each in list order exactly as the person form would; the bar shows how many were graded.
// The count is cleared while it runs. A refusal shows the error and changes nothing.
function BulkGrade({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheetId);
  const [user] = useUser();
  const [grade, setGrade] = useState<Grade>('A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [graded, setGraded] = useState<number | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const apply = useMutation({
    mutationFn: (personIds: string[]) =>
      postJson<{ graded: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/bulk-grade`,
        { personIds, grade, gradeFrom },
        user,
      ),
    onSuccess: (r) => {
      setGraded(r.graded);
      clearSelectedPeople(sheetId);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('The selected people could not be given that grade.'),
  });

  const press = () => {
    setGraded(null);
    setError('');
    const ids = [...selected];
    ids.forEach(clearRowError);
    once((done) => apply.mutate(ids, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-people">
      <label>
        Grade{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            setError('');
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        From{' '}
        <input
          type="date"
          data-testid="bulk-grade-from"
          value={gradeFrom}
          onChange={(e) => {
            setGradeFrom(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="button" data-testid="bulk-grade-apply" onClick={press} disabled={apply.isPending}>
        Grade selected ({selected.size})
      </button>
      {graded !== null && (
        <>
          {' '}Graded <span data-testid="bulk-graded">{graded}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 7, Component: BulkGrade }),
  PersonHead.fill({ order: 1, Component: SelectHead }),
  PersonRow.fill({ order: 1, Component: PersonSelect }),
];
