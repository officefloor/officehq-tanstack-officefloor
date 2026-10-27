import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useSheetToday } from '../sheet/useSheetToday';
import { GRADES, peopleKey, peopleQuery } from './peopleQuery';
import type { Grade, Person } from './peopleQuery';
import { useSelectedPeople } from './useSelectedPeople';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Gives every ticked person a grade from a from-date (blank: the sheet's today), one at a time in list
 * order, through the same edit the person form saves, so each writes the same audit lines. A person the
 * server refuses is left as they were and not counted.
 */
function BulkGrade({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const [selected, setSelected] = useSelectedPeople();
  const [grade, setGrade] = useState<Grade>('A');
  const [from, setFrom] = useState('');
  const [error, setError] = useState('');
  const [graded, setGraded] = useState<number | undefined>();
  const once = useSubmitOnce();

  const apply = useMutation({
    mutationFn: async (targets: Person[]): Promise<number> => {
      let changed = 0;
      for (const p of targets) {
        try {
          await putJson<Person>(
            `/api/sheets/${encodeURIComponent(sheetId)}/people/${p.id}`,
            { name: p.name, grade, gradeFrom: from.trim(), terms: p.terms == null ? '' : String(p.terms), contact: p.contact ?? '' },
            user,
          );
          changed++;
        } catch {
          // refused: left as they were
        }
      }
      return changed;
    },
    onSuccess: async (n) => {
      setSelected([]);
      setFrom('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setGraded(n);
    },
    onError: () => setError('The grades could not be given.'),
  });

  return (
    <section data-testid="bulk-grade-panel">
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
      </select>{' '}
      <input
        data-testid="bulk-grade-from"
        placeholder={today || 'from yyyy-mm-dd'}
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={apply.isPending}
        onClick={() => {
          setError('');
          setGraded(undefined);
          const f = from.trim();
          if (f !== '' && !DATE.test(f)) {
            setError('The date must be yyyy-mm-dd.');
            return;
          }
          // In list order, whatever order they were ticked in.
          once(() => apply.mutateAsync(people.filter((p) => selected.includes(p.id))));
        }}
      >
        Grade selected
      </button>
      {graded !== undefined && (
        <>
          {' '}
          Changed <span data-testid="bulk-graded">{graded}</span>
        </>
      )}
      {error && <span data-testid="person-error">{error}</span>}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 12,
  Component: ({ user, sheetId }) => <BulkGrade key={sheetId} user={user} sheetId={sheetId} />,
});
