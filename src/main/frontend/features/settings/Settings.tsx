import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { useSheetToday } from '../sheet/api';
import { poolsKey } from '../pools/api';
import { ratesKey } from '../rates/api';
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
    onSettled: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) }),
    ]),
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
              {key === 'clerk-limit' ? 'Clerk limit' : key === 'cap' ? 'Cap' : key === 'pot' ? 'Pot' : key === 'alert-threshold' ? 'Alert threshold' : key === 'floor' ? 'Floor' : key === 'late-pct' ? 'Late percent'
                : key === 'policy-date' ? 'Policy date' : key === 'rounding' ? 'Rounding mode (half-up or half-even)' : key === 'period-numbering-from' ? 'Period numbering from'
                : key === 'entry-date' ? 'Entry date'
                : key === 'grace-days' ? 'Grace days'
                : key === 'proof-threshold' ? 'Proof threshold'
                : key === 'note-threshold' ? 'Note threshold'
                : key === 'review-threshold' ? 'Review threshold'
                : key === 'parent' ? 'Parent sheet'
                : key === 'fiscal-start-month' ? 'Fiscal year start month (1 to 12)'
                : key.startsWith('perm-') ? `Who may ${key.slice('perm-'.length)} (roles)`
                : key.startsWith('proof-threshold-') ? `Proof threshold for grade ${key.slice('proof-threshold-'.length)}`
                : key.startsWith('grace-') ? `Grace days for grade ${key.slice('grace-'.length)}`
                : key.startsWith('terms-') ? `Terms for grade ${key.slice('terms-'.length)} (business days)` : 'Terms (business days)'}{' '}
              <input data-testid={`setting-${key}`} value={form[key] ?? current(key)}
                placeholder={key === 'clerk-limit' ? 'No limit' : key === 'proof-threshold' || key === 'note-threshold' || key === 'review-threshold' ? 'None' : key.startsWith('proof-threshold-') ? "Sheet's threshold" : key === 'cap' ? 'No cap' : key === 'pot' || key === 'parent' ? 'None' : key === 'alert-threshold' ? 'Off' : key === 'rounding' ? 'None' : key === 'policy-date' ? 'Today (yyyy-MM-dd)'
                  : key === 'period-numbering-from' || key === 'entry-date' ? 'Off (yyyy-MM-dd)' : key === 'grace-days' ? 'Off' : key.startsWith('grace-') ? 'None' : key.startsWith('terms-') ? "Sheet's terms"
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
            The clerk limit, the cap, the pot, the proof threshold, a grade's proof threshold, the note threshold and the review threshold must be blank, and the floor given, as 0.00 or more with up to two decimals;
            the terms a whole number of days, and a grade's terms blank or a whole number of days; the late percent 0.00 or more with up to two decimals; the rounding mode blank, half-up or half-even; the policy date, period numbering date and entry date blank or a date; the grace days and a grade's grace days blank or a whole number of days; a permission blank or a comma separated list of clerk and steward; the parent sheet blank or a sheet that has been loaded, not this one nor one with this one above it; the fiscal year start month a whole number from 1 to 12; a from-date blank or a date.
          </span>
        )}
      </form>
    </section>
  );
}
