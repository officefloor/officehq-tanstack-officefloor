import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { useSheetToday } from '../sheet/api';
import { changeSetting, listSettings, PERMISSION_STANDING, settingOn, settingsKey, SETTING_KEYS, UNDATED_KEYS } from './api';

/** The sheet's settings: each one's value in force on the sheet's today and a form that changes them.
 *  A field shows the value in force until it is edited; saving saves the fields entered (every field
 *  when none was), a dated one from its from-date (blank: the sheet's today). */
export function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const today = useSheetToday(sheetId, user);
  const current = (key: string) => settingOn(settings.data, key, today) ?? '';
  const [form, setForm] = useState<Record<string, string>>({});
  const [froms, setFroms] = useState<Record<string, string>>({});

  const save = useMutation({
    mutationFn: async ({ values, from }: { values: Record<string, string>; from: Record<string, string> }) => {
      for (const [key, value] of Object.entries(values)) await changeSetting(sheetId, user, key, value, from[key] ?? '');
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) }),
  });

  const submitOnce = useSubmitOnce();

  const enter = (key: string) => setForm((f) => (key in f ? f : { ...f, [key]: current(key) }));

  const change = (key: string, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    save.reset();
  };

  // A from-date entered alone saves the value in force from it.
  const changeFrom = (key: string, from: string) => {
    enter(key);
    setFroms((f) => ({ ...f, [key]: from }));
    save.reset();
  };

  return (
    <section>
      <h3>Settings</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const values = Object.keys(form).length > 0 ? form
            : Object.fromEntries(SETTING_KEYS.map((key) => [key, current(key)]));
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ values, from: froms }).then(() => { setForm({}); setFroms({}); });
          });
        }}
      >
        {SETTING_KEYS.map((key) => (
          <div key={key}>
            <label>
              {key === 'clerk-limit' ? 'Clerk limit' : key === 'cap' ? 'Cap' : key === 'floor' ? 'Floor' : key === 'late-pct' ? 'Late percent'
                : key === 'policy-date' ? 'Policy date' : key === 'period-numbering-from' ? 'Period numbering from'
                : key === 'proof-threshold' ? 'Proof threshold'
                : key.startsWith('perm-') ? `Who may ${key.slice('perm-'.length)} (roles)`
                : key.startsWith('terms-') ? `Terms for grade ${key.slice('terms-'.length)} (days)` : 'Terms (days)'}{' '}
              <input data-testid={`setting-${key}`} value={form[key] ?? current(key)}
                placeholder={key === 'clerk-limit' ? 'No limit' : key === 'proof-threshold' ? 'None' : key === 'cap' ? 'No cap' : key === 'policy-date' ? 'Today (yyyy-MM-dd)'
                  : key === 'period-numbering-from' ? 'Off (yyyy-MM-dd)' : key.startsWith('terms-') ? "Sheet's terms"
                  : key.startsWith('perm-') ? PERMISSION_STANDING[key.slice('perm-'.length)] : undefined}
                onFocus={() => enter(key)} onChange={(e) => change(key, e.target.value)} />
            </label>{' '}
            {!UNDATED_KEYS.includes(key) && (
              <>
                <label>
                  from{' '}
                  <input type="date" data-testid={`setting-${key}-from`} value={froms[key] ?? ''}
                    onChange={(e) => changeFrom(key, e.target.value)} />
                </label>{' '}
              </>
            )}
            Now: <span data-testid={`setting-${key}-current`}>{current(key)}</span>
          </div>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The clerk limit and the cap must be blank, and the floor given, as 0.00 or more with up to two decimals;
            the terms a whole number of days, and a grade's terms blank or a whole number of days; the late percent 0.00 or more with up to two decimals; the policy date and period numbering date blank or a date; a permission blank or a comma separated list of clerk and steward; a from-date blank or a date.
          </span>
        )}
      </form>
    </section>
  );
}
