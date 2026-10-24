import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { settingsPath, settingsQueryKey, useSettings, type Settings } from './roles';

// The sheet's settings: the clerk limit in force (blank when there is none) and the form that sets it.
// A clerk settles a claim only when its due is at or below the limit; a steward settles anything.
function SheetSettings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings();
  const [limit, setLimit] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { clerkLimit: string }) => putJson<Settings>(settingsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: settingsQueryKey(sheet ?? '') });
      setLimit('');
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet) return null;
  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</span>
      </p>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ clerkLimit: limit });
        }}
      >
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank: no limit)"
          inputMode="decimal"
          value={limit}
          onChange={(e) => { setLimit(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 8, Component: SheetSettings });
