import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { AMOUNT, DAYS, DEFAULT_FLOOR, DEFAULT_TERMS, settingsKey, useSettings } from './settings';
import type { Settings } from './settings';
import { useOneAtATime } from '../../query/useOneAtATime';

/** The settings the form sends: only those whose field was touched since the last save. */
type SettingsInput = { clerkLimit?: string; floor?: string; terms?: string };

type Touched = { clerkLimit: boolean; floor: boolean; terms: boolean };

const UNTOUCHED: Touched = { clerkLimit: false, floor: false, terms: false };

// The sheet's clerk limit (blank: no limit), floor and terms in force, and the form that sets them. Saving sends
// only the fields touched since the last save, so a blank field left alone changes nothing.
export function SettingsForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings().data;
  const [clerkLimit, setClerkLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [touched, setTouched] = useState<Touched>(UNTOUCHED);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: SettingsInput) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setClerkLimit('');
      setFloor('');
      setTerms('');
      setTouched(UNTOUCHED);
      setError('');
      queryClient.setQueryData(settingsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
    },
    onError: () => setError('That setting could not be saved.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const input: SettingsInput = {};
    if (touched.clerkLimit) {
      const value = clerkLimit.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a clerk limit of 0.00 or more, with at most two decimals, or leave it blank for no limit.');
        return;
      }
      input.clerkLimit = value;
    }
    if (touched.floor) {
      const value = floor.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError(`Type a floor of 0.00 or more, with at most two decimals, or leave it blank for ${DEFAULT_FLOOR}.`);
        return;
      }
      input.floor = value;
    }
    if (touched.terms) {
      const value = terms.trim();
      if (value !== '' && !DAYS.test(value)) {
        setError(`Type terms of 0 or more whole days, or leave them blank for ${DEFAULT_TERMS}.`);
        return;
      }
      input.terms = value;
    }
    if (input.clerkLimit === undefined && input.floor === undefined && input.terms === undefined) return;
    once(() => save.mutateAsync(input));
  };

  return (
    <section data-testid="settings">
      <p>
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>
      </p>
      <p>
        Floor: <span data-testid="setting-floor-current">{settings?.floor ?? ''}</span>
      </p>
      <p>
        Terms (days): <span data-testid="setting-terms-current">{settings?.terms ?? ''}</span>
      </p>
      <form onSubmit={submit} data-testid="settings-form">
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank: no limit)"
          value={clerkLimit}
          onFocus={() => setTouched((t) => ({ ...t, clerkLimit: true }))}
          onChange={(e) => {
            setClerkLimit(e.target.value);
            setTouched((t) => ({ ...t, clerkLimit: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-floor"
          placeholder={`Floor (blank: ${DEFAULT_FLOOR})`}
          value={floor}
          onFocus={() => setTouched((t) => ({ ...t, floor: true }))}
          onChange={(e) => {
            setFloor(e.target.value);
            setTouched((t) => ({ ...t, floor: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-terms"
          placeholder={`Terms in days (blank: ${DEFAULT_TERMS})`}
          value={terms}
          onFocus={() => setTouched((t) => ({ ...t, terms: true }))}
          onChange={(e) => {
            setTerms(e.target.value);
            setTouched((t) => ({ ...t, terms: true }));
            setError('');
          }}
        />
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <span data-testid="settings-error">{error}</span>}
      </form>
    </section>
  );
}
