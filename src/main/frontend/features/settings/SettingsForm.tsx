import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { AMOUNT, settingsKey, useSettings } from './settings';
import type { Settings } from './settings';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's clerk limit in force (blank: no limit) and the form that sets it.
export function SettingsForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings().data;
  const [clerkLimit, setClerkLimit] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (value: string) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings?user=${encodeURIComponent(user)}`, {
        clerkLimit: value,
      }),
    onSuccess: async (saved) => {
      setClerkLimit('');
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
    const value = clerkLimit.trim();
    if (value !== '' && !AMOUNT.test(value)) {
      setError('Type a clerk limit of 0.00 or more, with at most two decimals, or leave it blank for no limit.');
      return;
    }
    once(() => save.mutateAsync(value));
  };

  return (
    <section data-testid="settings">
      <p>
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>
      </p>
      <form onSubmit={submit} data-testid="settings-form">
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank: no limit)"
          value={clerkLimit}
          onChange={(e) => {
            setClerkLimit(e.target.value);
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
