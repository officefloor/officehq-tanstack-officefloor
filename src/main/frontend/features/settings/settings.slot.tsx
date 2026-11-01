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
// a clerk may settle (two decimals); blank means no limit. The floor is the least a claim's live due is (two
// decimals); blank takes its default, 1.00. Saving sends each setting whose field was changed; with none changed it
// sends the clerk limit, so saving a blank form clears the limit.
function Settings({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const [clerkLimit, setClerkLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [changed, setChanged] = useState<SettingKey[]>([]);

  const save = useMutation({
    mutationFn: async (bodies: { key: SettingKey; value: string }[]) => {
      let updated: SheetSettings | undefined;
      for (const body of bodies) {
        updated = await postJson<SheetSettings>(byUser(settingsPath(sheet), user), body);
        queryClient.setQueryData(settingsKey(sheet), updated);
      }
      return updated;
    },
    onSuccess: () => {
      setClerkLimit('');
      setFloor('');
      setChanged([]);
    },
  });
  const send = useSubmitOnce(save);

  const touch = (key: SettingKey) => {
    setChanged((keys) => (keys.includes(key) ? keys : [...keys, key]));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a value that is not 0.00 or more with up to two decimals.
    const values: Record<SettingKey, string> = { 'clerk-limit': clerkLimit, floor };
    const keys: SettingKey[] = changed.length === 0 ? ['clerk-limit'] : changed;
    send(keys.map((key) => ({ key, value: values[key] })));
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
              touch('clerk-limit');
            }}
          />
        </label>{' '}
        now <span data-testid="setting-clerk-limit-current">{data?.clerkLimit ?? ''}</span>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            placeholder="0.00 (blank: 1.00)"
            value={floor}
            onChange={(e) => {
              setFloor(e.target.value);
              touch('floor');
            }}
          />
        </label>{' '}
        now <span data-testid="setting-floor-current">{data?.floor ?? ''}</span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit and the floor must be 0.00 or more with up to two decimals, or blank.
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: Settings });
