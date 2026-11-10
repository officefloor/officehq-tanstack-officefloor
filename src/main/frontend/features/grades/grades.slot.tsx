import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, useGrades } from '../people/usePeople';
import { settingsKey, useSettings, type Settings } from '../settings/useSettings';

/** The sheet's grades, each shown with whether it is retired, the form to retire one (one already retired is
 *  refused), and the form to merge one into another (a steward only; the same grade twice is refused): the server
 *  gives everyone on the merged grade the other from the sheet's today, and the panel shows how many it moved
 *  (gone while it runs). A retired grade cannot be chosen for a person any more; people already on it keep it.
 *  A grade can be added: a code of one to three capital letters not yet a grade of the sheet; it is listed after the
 *  others, and offered by every grade select. The three forms share the one error, the latest refusal. */
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
  const [code, setCode] = useState('');
  const addOnce = useSingleFlight();
  const add = useMutation({
    mutationFn: (f: { grade: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`, f),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey(sheetId), saved);
      setCode('');
    },
  });
  const grades = useGrades(sheetId);
  const retired = settings.data?.retiredGrades ?? [];
  const error = retire.isError ? retire.error : merge.isError ? merge.error : add.isError ? add.error : null;
  const resetAll = () => {
    retire.reset();
    merge.reset();
    add.reset();
  };

  return (
    <section data-testid="grades">
      <ul data-testid="grades-list">
        {grades.map((g) => (
          <li key={g} data-testid={`grade-row-${g}`}>
            Grade {g}, retired: <span data-testid="grade-retired">{retired.includes(g) ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
      <form
        data-testid="grade-add-form"
        onSubmit={(e) => {
          e.preventDefault();
          addOnce(() => {
            resetAll();
            return add.mutateAsync({ grade: code });
          });
        }}
      >
        <label>
          Add grade{' '}
          <input
            data-testid="grade-add-code"
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              resetAll();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add
        </button>
      </form>
      <form
        data-testid="grade-retire-form"
        onSubmit={(e) => {
          e.preventDefault();
          retireOnce(() => {
            resetAll();
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
              resetAll();
            }}
          >
            <option value="">—</option>
            {grades.map((g) => (
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
            resetAll();
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
              resetAll();
            }}
          >
            <option value="">—</option>
            {grades.map((g) => (
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
              resetAll();
            }}
          >
            <option value="">—</option>
            {grades.map((g) => (
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
