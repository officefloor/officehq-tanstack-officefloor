import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { sheetKey, sheetQuery } from './sheetQuery';
import { plansKey } from '../plans/plansQuery';
import { periodsKey } from '../periods/periodsQuery';
import type { Sheet } from './sheetQuery';
import { useSheetToday } from './useSheetToday';
import { settingsQuery } from '../settings/settingsQuery';

function SheetToday({ sheetId }: { sheetId: string }) {
  const today = useSheetToday(sheetId);
  const archived = useQuery(sheetQuery(sheetId)).data?.archived;
  const parent = useQuery(settingsQuery(sheetId)).data?.parent ?? '';
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const [error, setError] = useState('');
  const [cloneTo, setCloneTo] = useState('');
  const [clonedTo, setClonedTo] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (date: string) =>
      putJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { today: date }, user),
    onSuccess: async (sheet) => {
      queryClient.setQueryData(sheetKey(sheetId), sheet);
      // The change may have closed periods past their grace days,
      // and moved the plans' next dates.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
      ]);
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'That is not a date (yyyy-mm-dd).'),
  });

  const archive = useMutation({
    mutationFn: () => putJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, {}, user),
    onSuccess: (sheet) => queryClient.setQueryData(sheetKey(sheetId), sheet),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The sheet could not be archived.'),
  });

  const clone = useMutation({
    mutationFn: (to: string) =>
      postJson<{ toSheetId: string }>(`/api/sheets/${encodeURIComponent(sheetId)}/clone`, { toSheetId: to }, user),
    onSuccess: (cloned) => setClonedTo(cloned.toSheetId),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The sheet could not be cloned.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync(typed.trim()));
  };

  return (
    <form data-testid="sheet-today-form" onSubmit={submit}>
      Today <strong data-testid="sheet-today">{today}</strong>{' '}
      <label>
        Change to{' '}
        <input
          data-testid="sheet-today-input"
          placeholder="yyyy-mm-dd"
          value={typed}
          onChange={(e) => {
            setTyped(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save">
        Set today
      </button>{' '}
      Archived <strong data-testid="sheet-archived">{archived ? 'yes' : 'no'}</strong>{' '}
      Parent <strong data-testid="sheet-parent">{parent}</strong>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        onClick={() => {
          setError('');
          once(() => archive.mutateAsync());
        }}
      >
        Archive sheet
      </button>{' '}
      <label>
        Clone to{' '}
        <input
          data-testid="sheet-clone-to"
          value={cloneTo}
          onChange={(e) => {
            setCloneTo(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="sheet-clone"
        onClick={() => {
          setError('');
          // The last clone made is cleared while the next runs, so two clones to one id are told apart.
          setClonedTo('');
          once(() => clone.mutateAsync(cloneTo.trim()));
        }}
      >
        Clone sheet
      </button>{' '}
      Cloned to <strong data-testid="sheet-cloned-to">{clonedTo}</strong>
      {error && <p data-testid="sheet-error">{error}</p>}
    </form>
  );
}

// Keyed by sheet, so a typed date or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 0,
  Component: ({ sheetId }) => <SheetToday key={sheetId} sheetId={sheetId} />,
});
