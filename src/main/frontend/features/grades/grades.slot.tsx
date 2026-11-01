import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradesKey, useGrades, type Grade } from './useGrades';

/**
 * The sheet's grades, each shown retired or not, an entry to add one (a code of one to three capital
 * letters, the spaces around it aside, not yet a grade; anything else is refused), and a select to retire one. A retired grade cannot
 * be chosen for a person any more (the people already on it keep it); retiring one twice is refused.
 * A steward can merge a grade into another: everyone on it today is given the other from the sheet's
 * today. The merge shows the grades and how many it moved; that goes while the next press runs.
 */
function GradesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: grades = [] } = useGrades(sheet);
  const [code, setCode] = useState('');
  const [grade, setGrade] = useState('A');
  const [mergeFrom, setMergeFrom] = useState('A');
  const [mergeInto, setMergeInto] = useState('B');

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (g: string) => postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`, { grade: g, user }),
    onSuccess: async () => {
      setCode('');
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
  });

  const resetAll = () => {
    add.reset();
    retire.reset();
    merge.reset();
  };

  const retire = useMutation({
    mutationFn: (g: string) => postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire`, { grade: g, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
  });

  const merge = useMutation({
    mutationFn: (m: { from: string; into: string }) =>
      postJson<{ from: string; into: string; moved: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/grades/merge`,
        { ...m, user },
      ),
    // Everyone moved changes the people, their claims' figures and the sheet's totals.
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheet] }),
  });

  return (
    <section data-testid="grades-panel">
      <h2>Grades</h2>
      <form
        data-testid="grade-add-form"
        onSubmit={(e) => {
          e.preventDefault();
          resetAll();
          submitOnce(() => add.mutateAsync(code));
        }}
      >
        <label>
          New grade{' '}
          <input
            data-testid="grade-add-code"
            placeholder="D"
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add grade
        </button>
        {add.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}
            Could not add this grade.
          </span>
        )}
      </form>
      <form
        data-testid="grade-retire-form"
        onSubmit={(e) => {
          e.preventDefault();
          resetAll();
          submitOnce(() => retire.mutateAsync(grade));
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="grade-retire-select"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              retire.reset();
            }}
          >
            {grades.map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}
            Could not retire this grade.
          </span>
        )}
      </form>
      <form
        data-testid="grade-merge-form"
        onSubmit={(e) => {
          e.preventDefault();
          resetAll();
          submitOnce(() => merge.mutateAsync({ from: mergeFrom, into: mergeInto }));
        }}
      >
        <label>
          Merge{' '}
          <select
            data-testid="grade-merge-from"
            value={mergeFrom}
            onChange={(e) => {
              setMergeFrom(e.target.value);
              merge.reset();
            }}
          >
            {grades.map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
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
              merge.reset();
            }}
          >
            {grades.map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge grade
        </button>
        {merge.isSuccess && (
          <span>
            {' '}
            Moved <strong data-testid="grade-merged">{`${merge.data.from} into ${merge.data.into}: ${merge.data.moved}`}</strong>
          </span>
        )}
        {merge.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}
            Could not merge these grades.
          </span>
        )}
      </form>
      <table data-testid="grade-list">
        <tbody>
          {grades.map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td data-testid="grade-code">{g.grade}</td>
              <td data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 7, Component: GradesPanel });
