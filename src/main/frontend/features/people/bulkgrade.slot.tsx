import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey } from '../claims/claims';
import { useGradeCodes } from '../grades/grades';
import { useChangeToday } from '../sheet/sheet';
import { peopleKey, usePeople, useShowRetired } from './people';
import { clearSelectedPeople, useSelectedPeople } from './selection';

type BulkGradeResult = { changed: number };

/** The bar that gives every selected person a grade together from a from-date (the sheet's today unless
 *  changed). The server grades each, in list order, as the person form's edit would; the bar then shows how
 *  many were changed. */
function BulkGrade({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [showRetired] = useShowRetired();
  const chosen = useSelectedPeople(sheetId);
  const ids = people.filter((p) => (showRetired || p.retiredFrom === null) && chosen.has(p.id)).map((p) => p.id);
  const today = useChangeToday(sheetId);
  const grades = useGradeCodes(sheetId);
  const [grade, setGrade] = useState('A');
  const [fromEntered, setFrom] = useState<string>();
  const from = fromEntered ?? today;
  const [result, setResult] = useState<BulkGradeResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const apply = useMutation({
    mutationFn: (body: { user: string; personIds: number[]; grade: string; from?: string }) =>
      postJson<BulkGradeResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-grade`, body),
    onSuccess: async (r) => {
      clearSelectedPeople(sheetId);
      setFrom(undefined);
      setResult(r);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not grade the selected people.'),
    onSettled: once.done,
  });

  return (
    <p>
      <label>
        Grade selected{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            setError('');
            setResult(null);
          }}
        >
          {grades.map((g) => (
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
            setError('');
            setResult(null);
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={apply.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the people to grade.');
            return;
          }
          if (!once.start()) return;
          apply.mutate({ user, personIds: ids, grade, from: from.trim() === '' ? undefined : from });
        }}
      >
        Grade
      </button>
      {result && (
        <>
          {' '}Changed <span data-testid="bulk-graded">{result.changed}</span>
        </>
      )}
      {error && (
        <span data-testid="person-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11.5, Component: BulkGrade });
