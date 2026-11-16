import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { useSheetToday } from '../sheet/api';
import { poolsKey } from '../pools/api';
import { ratesKey } from '../rates/api';
import { changeSetting, listSettings, PERMISSION_STANDING, usePolicyVersions, settingOn, settingsKey, SETTING_KEYS, UNDATED_KEYS } from './api';
import { Fig } from '../session/Fig';

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
  const policy = usePolicyVersions(sheetId);

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
      <p>Policy version <span data-testid="policy-version">{policy.current}</span></p>
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
                : key === 'escalate-after' ? 'Escalate after (days)'
                : key === 'review-sla' ? 'Review SLA (days)'
                : key === 'pending-sla' ? 'Pending SLA (days)'
                : key === 'max-open' ? 'Most open claims per person'
                : key === 'score-mid' ? 'Score mid threshold'
                : key === 'score-high' ? 'Score high threshold'
                : key === 'score-log' ? 'Score log (on)'
                : key === 'proof-threshold' ? 'Proof threshold'
                : key === 'proof-kinds' ? 'Proof kinds (receipt, photo)'
                : key === 'note-threshold' ? 'Note threshold'
                : key === 'review-threshold' ? 'Review threshold'
                : key === 'parent' ? 'Parent sheet'
                : key === 'fiscal-start-month' ? 'Fiscal year start month (1 to 12)'
                : key === 'default-grade' ? 'Default grade (a grade of the sheet)'
                : key.startsWith('perm-') ? `Who may ${key.slice('perm-'.length)} (roles)`
                : key.startsWith('clerk-limit-') ? `Clerk limit for grade ${key.slice('clerk-limit-'.length)}`
                : key.startsWith('proof-threshold-') ? `Proof threshold for grade ${key.slice('proof-threshold-'.length)}`
                : key.startsWith('grace-') ? `Grace days for grade ${key.slice('grace-'.length)}`
                : key.startsWith('terms-') ? `Terms for grade ${key.slice('terms-'.length)} (business days)` : 'Terms (business days)'}{' '}
              <input data-testid={`setting-${key}`} value={form[key] ?? current(key)}
                placeholder={key === 'clerk-limit' ? 'No limit' : key === 'proof-threshold' || key === 'note-threshold' || key === 'review-threshold' || key === 'review-sla' || key === 'pending-sla' || key === 'max-open' || key === 'score-mid' || key === 'score-high' ? 'None' : key.startsWith('proof-threshold-') ? "Sheet's threshold" : key.startsWith('clerk-limit-') ? "Sheet's limit" : key === 'cap' ? 'No cap' : key === 'pot' || key === 'parent' || key === 'proof-kinds' ? 'None' : key === 'alert-threshold' ? 'Off' : key === 'rounding' ? 'None' : key === 'policy-date' ? 'Today (yyyy-MM-dd)'
                  : key === 'period-numbering-from' || key === 'entry-date' ? 'Off (yyyy-MM-dd)' : key === 'grace-days' || key === 'escalate-after' || key === 'score-log' ? 'Off' : key.startsWith('grace-') ? 'None' : key.startsWith('terms-') ? "Sheet's terms"
                  : key === 'default-grade' ? 'A'
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
            Now: <span data-testid={`setting-${key}-current`}><Fig>{current(key)}</Fig></span>
          </div>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The clerk limit, a grade's clerk limit, the cap, the pot, the proof threshold, a grade's proof threshold, the note threshold and the review threshold must be blank, and the floor given, as 0.00 or more with up to two decimals;
            the terms a whole number of days, and a grade's terms blank or a whole number of days; the late percent 0.00 or more with up to two decimals; the rounding mode blank, half-up or half-even; the policy date, period numbering date and entry date blank or a date; the grace days, a grade's grace days and the review SLA blank or a whole number of days; the most open claims per person blank or a whole number, 0 or more; a score threshold blank or a whole number; the score log blank or on; a permission blank or a comma separated list of clerk and steward; the proof kinds blank or a comma separated list of receipt and photo; the parent sheet blank or a sheet that has been loaded, not this one nor one with this one above it; the fiscal year start month a whole number from 1 to 12; the default grade blank or a grade of the sheet; a from-date blank or a date.
          </span>
        )}
      </form>
    </section>
  );
}
