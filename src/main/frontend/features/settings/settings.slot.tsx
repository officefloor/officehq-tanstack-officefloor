import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { settingsKey, settingsPath, useSettings } from './settings';
import type { SettingKey, Settings as SheetSettings } from './settings';

// The sheet's settings: each value in force, and a form that changes it. The clerk limit is the most due
// a clerk may settle (two decimals); blank means no limit.
function Settings({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const [clerkLimit, setClerkLimit] = useState('');

  const save = useMutation({
    mutationFn: (body: { key: SettingKey; value: string }) =>
      postJson<SheetSettings>(byUser(settingsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(settingsKey(sheet), updated);
      setClerkLimit('');
    },
  });
  const send = useSubmitOnce(save);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a limit that is not 0.00 or more with up to two decimals.
    send({ key: 'clerk-limit', value: clerkLimit });
  };

  return (
    <section>
      <h2>Settings</h2>
      <form onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="0.00 (blank: no limit)"
            value={clerkLimit}
            onChange={(e) => {
              setClerkLimit(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        now <span data-testid="setting-clerk-limit-current">{data?.clerkLimit ?? ''}</span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit must be 0.00 or more with up to two decimals, or blank for no limit.
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: Settings });
