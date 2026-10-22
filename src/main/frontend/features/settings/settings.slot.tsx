import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';

/** The sheet's settings: the clerk limit with two decimals ('' when there is none). */
export type Settings = { clerkLimit: string };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

const LIMIT = /^\d+(\.\d{1,2})?$/;

/** The sheet's settings — the clerk limit in force, and the form that saves it (blank: no limit). A clerk may
 *  settle only claims whose due is at or below it; the server enforces it. */
function SettingsSection({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
  const current = settings.data?.clerkLimit ?? '';
  // null: not typed in since the last save, so the field shows the limit in force.
  const [typed, setTyped] = useState<string | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (clerkLimit: string) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, { user, clerkLimit }),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey(sheetId), saved);
      setTyped(null);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body
        ? e.body
        : 'Could not save the settings: the clerk limit must be an amount with up to two decimals, or blank.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const limit = (typed ?? current).trim();
    if (limit !== '' && !LIMIT.test(limit)) {
      setError('The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.');
      return;
    }
    if (!once.start()) return;
    save.mutate(limit);
  };

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit <strong data-testid="setting-clerk-limit-current">{current}</strong>
      </p>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="no limit"
            value={typed ?? current}
            onChange={(e) => {
              setTyped(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && (
          <p data-testid="settings-error" role="alert">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7, Component: SettingsSection });
