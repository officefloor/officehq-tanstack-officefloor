import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { DEFAULT_FLOOR, settingsKey, useSettings, type Settings } from './settings';

const LIMIT = /^\d+(\.\d{1,2})?$/;

/** The settings a save changes: only those typed into since the last save are sent (and recorded). */
type Changes = { clerkLimit?: string; floor?: string };

/** The sheet's settings — the clerk limit and the floor in force, and the form that saves them (a blank clerk
 *  limit: no limit). A clerk may settle only claims whose due is at or below the limit; the server enforces it.
 *  A claim's live due is at least the floor. */
function SettingsSection({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const currentLimit = settings.data?.clerkLimit ?? '';
  const currentFloor = settings.data?.floor ?? DEFAULT_FLOOR;
  // null: not typed in since the last save, so the field shows the setting in force.
  const [typedLimit, setTypedLimit] = useState<string | null>(null);
  const [typedFloor, setTypedFloor] = useState<string | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (changes: Changes) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, { user, ...changes }),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey(sheetId), saved);
      setTypedLimit(null);
      setTypedFloor(null);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body
        ? e.body
        : 'Could not save the settings: the clerk limit and floor must be amounts with up to two decimals.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const changes: Changes = {};
    if (typedLimit !== null) {
      const limit = typedLimit.trim();
      if (limit !== '' && !LIMIT.test(limit)) {
        setError('The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.');
        return;
      }
      changes.clerkLimit = limit;
    }
    if (typedFloor !== null) {
      const floor = typedFloor.trim();
      if (!LIMIT.test(floor)) {
        setError('The floor must be an amount, 0.00 or more, with up to two decimals.');
        return;
      }
      changes.floor = floor;
    }
    if (!once.start()) return;
    save.mutate(changes);
  };

  // onInput as well as onChange: retyping the value in force still counts as setting it.
  const typed = (set: (v: string) => void) => (e: FormEvent<HTMLInputElement>) => {
    set(e.currentTarget.value);
    setError('');
  };

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit <strong data-testid="setting-clerk-limit-current">{currentLimit}</strong>{' '}
        Floor <strong data-testid="setting-floor-current">{currentFloor}</strong>
      </p>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="no limit"
            value={typedLimit ?? currentLimit}
            onInput={typed(setTypedLimit)}
            onChange={typed(setTypedLimit)}
          />
        </label>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            value={typedFloor ?? currentFloor}
            onInput={typed(setTypedFloor)}
            onChange={typed(setTypedFloor)}
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
