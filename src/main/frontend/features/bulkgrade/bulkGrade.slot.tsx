import { useState, useSyncExternalStore } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/people.slot';

// The people selected for a bulk action. Kept in a small store rather than the URL so a checkbox
// shows its new state on the same click (a URL update lands a render later). It belongs to one sheet:
// loading another starts with nothing selected.
const NONE: ReadonlySet<number> = new Set();
let selected: { sheetId: string; ids: ReadonlySet<number> } = { sheetId: '', ids: NONE };
const listeners = new Set<() => void>();
const selection = {
  subscribe: (l: () => void) => {
    listeners.add(l);
    return () => {
      listeners.delete(l);
    };
  },
  get: () => selected,
  set: (sheetId: string, ids: ReadonlySet<number>) => {
    selected = { sheetId, ids };
    listeners.forEach((l) => l());
  },
};
function useSelected(sheetId: string): ReadonlySet<number> {
  const current = useSyncExternalStore(selection.subscribe, selection.get);
  return current.sheetId === sheetId ? current.ids : NONE;
}

function SelectHead() {
  return <th>Select</th>;
}

// A person's checkbox selecting them for a bulk action.
function PersonSelect({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const chosen = useSelected(sheetId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={chosen.has(personId)}
        onChange={(e) => {
          const next = new Set(chosen);
          if (e.target.checked) next.add(personId);
          else next.delete(personId);
          selection.set(sheetId, next);
        }}
      />
    </td>
  );
}

const GRADES = ['A', 'B', 'C'] as const;

type BulkGradeResult = { changed: number };

// Gives the selected people a grade together from a date (blank: the sheet's today); the server grades them in
// list order exactly as the person form would. Shows how many were changed; the count is gone while it runs.
function BulkGrade({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [grade, setGrade] = useState('A');
  const [from, setFrom] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const apply = useMutation({
    mutationFn: (ids: number[]) => {
      if (ids.length === 0) throw new Error('Select the people to grade');
      return postJson<BulkGradeResult>(`/api/sheets/${encodeURIComponent(sheetId)}/people/bulk-grade`, {
        ids,
        grade,
        from: from.trim(),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      // A grade change can move the dues of the people's claims.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  return (
    <section data-testid="bulk-grade-actions">
      <label>
        Grade selected{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            apply.reset();
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
        from{' '}
        <input
          type="date"
          data-testid="bulk-grade-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            apply.reset();
          }}
        />
      </label>{' '}
      <button type="button" data-testid="bulk-grade-apply" disabled={apply.isPending} onClick={() => apply.mutate([...chosen])}>
        Give grade
      </button>{' '}
      {apply.isSuccess && (
        <span>
          Changed <strong data-testid="bulk-graded">{apply.data.changed}</strong>
        </span>
      )}
      {apply.isError && (
        <span data-testid="person-error" role="alert">
          {apply.error.message}
        </span>
      )}
    </section>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 0, Component: SelectHead }),
  PersonCells.fill({ order: 0, Component: PersonSelect }),
  SheetBody.fill({ order: 12, Component: BulkGrade }),
];
