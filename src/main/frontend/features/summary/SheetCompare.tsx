import { useState } from 'react';
import { getLiveJson, HttpError } from '../../api/http';
import type { Claim } from '../claims/claims';
import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useGradeCodes } from '../grades/grades';
import { usePeople } from '../people/people';
import type { Rate } from '../rates/rates';
import { useRates } from '../rates/rates';
import type { SheetInfo } from '../session/session';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Settings } from '../settings/settings';
import { dueRules, useDueRules } from '../settings/settings';
import { gradeDueTotal } from './Summary';

// Compare this sheet with another by id: one line per grade of this sheet (A to C then those added on it),
// `<grade> <this sheet's summary due-total> <the other's>`, the other's worked out as its own summary shows it (0.00
// for a grade it has no one on). A sheet that does not exist gives no lines; the other sheet is only read, never created.
export function SheetCompare() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const grades = useGradeCodes();
  const [other, setOther] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');

  if (!user || !sheet) return null;

  const compare = async () => {
    setLines(null);
    setError('');
    const path = `/api/sheets/${encodeURIComponent(other.trim())}`;
    try {
      let info: SheetInfo;
      try {
        info = await getLiveJson<SheetInfo>(path);
      } catch (e) {
        if (e instanceof HttpError && e.status === 404) {
          setLines([]);
          return;
        }
        throw e;
      }
      const [otherPeople, otherClaims, otherRates, settings] = await Promise.all([
        getLiveJson<Person[]>(`${path}/people`),
        getLiveJson<Claim[]>(`${path}/claims`),
        getLiveJson<Rate[]>(`${path}/rates`),
        getLiveJson<Settings>(`${path}/settings`),
      ]);
      const otherRules = dueRules(settings, info.today, otherClaims);
      setLines(
        grades.map(
          (g) =>
            `${g} ${gradeDueTotal(g, people, claims, rates, rules)} ${gradeDueTotal(g, otherPeople, otherClaims, otherRates, otherRules)}`,
        ),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  return (
    <section>
      <input data-testid="compare-sheet" value={other} placeholder="Compare with sheet" onChange={(e) => setOther(e.target.value)} />
      <button type="button" data-testid="compare" disabled={!other.trim()} onClick={compare}>
        Compare
      </button>
      {lines && (
        <ul>
          {lines.map((l) => (
            <li key={l} data-testid="compare-line">
              {l}
            </li>
          ))}
        </ul>
      )}
      {error && <span data-testid="compare-error">{error}</span>}
    </section>
  );
}
