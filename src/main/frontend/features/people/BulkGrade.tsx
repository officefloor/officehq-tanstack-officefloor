import { createContext, useContext, useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey } from './usePeople';

type Selection = { selected: Set<string>; toggle: (id: string, on: boolean) => void; clear: () => void };

const SelectionContext = createContext<Selection | null>(null);

/** The people ticked with their select checkbox, shared by every person row and the bulk grade. */
export function PersonSelection({ children }: { children: ReactNode }) {
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const toggle = (id: string, on: boolean) =>
    setSelected((was) => {
      const next = new Set(was);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  const clear = () => setSelected(new Set());
  return <SelectionContext.Provider value={{ selected, toggle, clear }}>{children}</SelectionContext.Provider>;
}

function usePersonSelection(): Selection {
  const selection = useContext(SelectionContext);
  if (!selection) throw new Error('usePersonSelection outside PersonSelection');
  return selection;
}

/** A person row's select checkbox. */
export function PersonSelect({ personId }: { personId: string }) {
  const { selected, toggle } = usePersonSelection();
  return (
    <input
      type="checkbox"
      data-testid="person-select"
      checked={selected.has(personId)}
      onChange={(e) => toggle(personId, e.target.checked)}
    />
  );
}

// Gives the selected people a grade together: the server records it for each, in list order, from the
// from-date (the sheet's today when blank) exactly as the person form would, with the same audit lines.
// Then shows how many were changed and clears the selection; pressing again removes the old count
// until the new one is in. A refusal (a bad date) shows the error and changes nothing; a viewer's grades no one.
export function BulkGrade() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const { selected, clear } = usePersonSelection();
  const [grade, setGrade] = useState<string>('A');
  const [gradeFrom, setGradeFrom] = useState('');

  const apply = useMutation({
    mutationFn: (personIds: string[]) =>
      postJson<{ graded: number }>(`/api/sheets/${encodeURIComponent(sheet)}/people/bulk-grade`, {
        personIds,
        grade,
        gradeFrom,
        user,
      }),
    onSuccess: () => {
      clear();
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
  });

  return (
    <p>
      <select
        data-testid="bulk-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          apply.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <input
        type="date"
        data-testid="bulk-grade-from"
        title="Grade from (blank: the sheet's today)"
        value={gradeFrom}
        onChange={(e) => {
          setGradeFrom(e.target.value);
          apply.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        onClick={() => {
          apply.reset();
          const ids = [...selected];
          once(() => apply.mutateAsync(ids));
        }}
      >
        Grade selected
      </button>
      {apply.isSuccess && (
        <>
          {' '}
          Changed <span data-testid="bulk-graded">{apply.data.graded}</span>
        </>
      )}
      {apply.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The selected people could not be given the grade.
        </span>
      )}
    </p>
  );
}
