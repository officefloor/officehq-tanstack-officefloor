import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Settings, settingsKey, useSettings } from './settings';

// The sheet's settings in force, and the form that changes them: the clerk limit (blank for none).
function SheetSettings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const [clerkLimit, setClerkLimit] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (value: string) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { key: 'clerk-limit', value, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
      setClerkLimit('');
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(clerkLimit);
        }}
      >
        Clerk limit <strong data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</strong>{' '}
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank for none)"
          inputMode="decimal"
          value={clerkLimit}
          onChange={(e) => {
            setClerkLimit(e.target.value);
            save.reset();
          }}
        />
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The settings were refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 48, Component: SheetSettings });
