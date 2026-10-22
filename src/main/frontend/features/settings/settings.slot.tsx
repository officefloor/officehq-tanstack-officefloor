import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, settingsKey, useSettings, type Settings } from './settings';

const LIMIT = /^\d+(\.\d{1,2})?$/;
const DAYS = /^\d{1,4}$/;
const PCT = /^\d{1,3}(\.\d{1,2})?$/;

/** The settings a save changes: only those typed into since the last save are sent (and recorded). */
type Changes = { clerkLimit?: string; floor?: string; terms?: string; latePct?: string };

/** The sheet's settings — the clerk limit and the floor in force, and the form that saves them (a blank clerk
 *  limit: no limit). A clerk may settle only claims whose due is at or below the limit; the server enforces it.
 *  A claim's live due is at least the floor; its due-by is its date plus the terms; once overdue it gains the late
 *  percent of its figure for every full 30 days. */
function SettingsSection({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const currentLimit = settings.data?.clerkLimit ?? '';
  const currentFloor = settings.data?.floor ?? DEFAULT_FLOOR;
  const currentTerms = settings.data?.terms ?? DEFAULT_TERMS;
  const currentLatePct = settings.data?.latePct ?? DEFAULT_LATE_PCT;
  // null: not typed in since the last save, so the field shows the setting in force.
  const [typedLimit, setTypedLimit] = useState<string | null>(null);
  const [typedFloor, setTypedFloor] = useState<string | null>(null);
  const [typedTerms, setTypedTerms] = useState<string | null>(null);
  const [typedLatePct, setTypedLatePct] = useState<string | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (changes: Changes) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, { user, ...changes }),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey(sheetId), saved);
      setTypedLimit(null);
      setTypedFloor(null);
      setTypedTerms(null);
      setTypedLatePct(null);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body
        ? e.body
        : 'Could not save the settings: the clerk limit and floor must be amounts with up to two decimals, the terms whole days, the late percent a percent.'),
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
    if (typedTerms !== null) {
      const terms = typedTerms.trim();
      if (!DAYS.test(terms)) {
        setError('The terms must be a whole number of days, 0 or more.');
        return;
      }
      changes.terms = terms;
    }
    if (typedLatePct !== null) {
      const latePct = typedLatePct.trim();
      if (!PCT.test(latePct)) {
        setError('The late percent must be a percent, 0.00 or more, with up to two decimals.');
        return;
      }
      changes.latePct = latePct;
    }
    if (!once.start()) return;
    save.mutate(changes);
  };

  // onInput as well as onChange, and focusing the field: retyping (or clearing) the value in force still counts
  // as setting it.
  const typed = (set: (v: string) => void) => (e: FormEvent<HTMLInputElement>) => {
    set(e.currentTarget.value);
    setError('');
  };

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit <strong data-testid="setting-clerk-limit-current">{currentLimit}</strong>{' '}
        Floor <strong data-testid="setting-floor-current">{currentFloor}</strong>{' '}
        Terms <strong data-testid="setting-terms-current">{currentTerms}</strong> days{' '}
        Late <strong data-testid="setting-late-pct-current">{currentLatePct}</strong>%
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
            onFocus={() => setTypedLimit((v) => v ?? currentLimit)}
          />
        </label>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            value={typedFloor ?? currentFloor}
            onInput={typed(setTypedFloor)}
            onChange={typed(setTypedFloor)}
            onFocus={() => setTypedFloor((v) => v ?? currentFloor)}
          />
        </label>{' '}
        <label>
          Terms (days){' '}
          <input
            data-testid="setting-terms"
            value={typedTerms ?? currentTerms}
            onInput={typed(setTypedTerms)}
            onChange={typed(setTypedTerms)}
            onFocus={() => setTypedTerms((v) => v ?? currentTerms)}
          />
        </label>{' '}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            value={typedLatePct ?? currentLatePct}
            onInput={typed(setTypedLatePct)}
            onChange={typed(setTypedLatePct)}
            onFocus={() => setTypedLatePct((v) => v ?? currentLatePct)}
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
