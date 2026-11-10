import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey } from '../people/usePeople';
import { settingsKey, useSettings, type Settings } from '../settings/useSettings';

/** The sheet's grades, each shown with whether it is retired, the form to retire one (one already retired is
 *  refused), and the form to merge one into another (a steward only; the same grade twice is refused): the server
 *  gives everyone on the merged grade the other from the sheet's today, and the panel shows how many it moved
 *  (gone while it runs). A retired grade cannot be chosen for a person any more; people already on it keep it.
 *  The two forms share the one error, the latest refusal. */
function GradesPanel({ sheetId }: { sheetId: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState('');
  const retireOnce = useSingleFlight();
  const retire = useMutation({
    mutationFn: (f: { grade: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, f),
    onSuccess: (saved) => queryClient.setQueryData(settingsKey(sheetId), saved),
  });
  const [mergeFrom, setMergeFrom] = useState('');
  const [mergeInto, setMergeInto] = useState('');
  const mergeOnce = useSingleFlight();
  const merge = useMutation({
    mutationFn: (f: { from: string; into: string }) =>
      postJson<{ merged: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/merge`, f),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const retired = settings.data?.retiredGrades ?? [];
  const error = retire.isError ? retire.error : merge.isError ? merge.error : null;

  return (
    <section data-testid="grades">
      <ul data-testid="grades-list">
        {GRADES.map((g) => (
          <li key={g} data-testid={`grade-row-${g}`}>
            Grade {g}, retired: <span data-testid="grade-retired">{retired.includes(g) ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
      <form
        data-testid="grade-retire-form"
        onSubmit={(e) => {
          e.preventDefault();
          retireOnce(() => {
            retire.reset();
            merge.reset();
            return retire.mutateAsync({ grade });
          });
        }}
      >
        <label>
          Retire grade{' '}
          <select
            data-testid="grade-retire-select"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              retire.reset();
              merge.reset();
            }}
          >
            <option value="">—</option>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire
        </button>
      </form>
      <form
        data-testid="grade-merge-form"
        onSubmit={(e) => {
          e.preventDefault();
          mergeOnce(() => {
            retire.reset();
            merge.reset();
            return merge.mutateAsync({ from: mergeFrom, into: mergeInto });
          });
        }}
      >
        <label>
          Merge grade{' '}
          <select
            data-testid="grade-merge-from"
            value={mergeFrom}
            onChange={(e) => {
              setMergeFrom(e.target.value);
              retire.reset();
              merge.reset();
            }}
          >
            <option value="">—</option>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          into{' '}
          <select
            data-testid="grade-merge-into"
            value={mergeInto}
            onChange={(e) => {
              setMergeInto(e.target.value);
              retire.reset();
              merge.reset();
            }}
          >
            <option value="">—</option>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge
        </button>
        {merge.isSuccess && (
          <>
            {' '}
            Moved <strong data-testid="grade-merged">{merge.data.merged}</strong>
          </>
        )}
      </form>
      {error && <p data-testid="grade-error">{error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <GradesPanel sheetId={sheetId} /> });
