import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { GRADES } from '../people/usePeople';
import { settingsKey, useSettings, type Settings } from '../settings/useSettings';

/** The sheet's grades, each shown with whether it is retired, and the form to retire one (one already retired is
 *  refused). A retired grade cannot be chosen for a person any more; people already on it keep it. */
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
  const retired = settings.data?.retiredGrades ?? [];

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
        {retire.isError && <p data-testid="grade-error">{retire.error.message}</p>}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <GradesPanel sheetId={sheetId} /> });
