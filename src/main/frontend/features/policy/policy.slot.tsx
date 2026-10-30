import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts, type Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { figure, ratesQuery, sumDue } from '../rates/ratesQuery';
import { policyOn, settingsQuery, useFloor, useLate, usePolicyVersionLines, usePolicyVersions } from '../settings/settingsQuery';

/** The sheet's current policy version: how many versions of its policy settings have been made. */
function PolicyVersion({ sheetId }: { sheetId: string }) {
  return (
    <p data-testid="policy-version-panel">
      Policy version <strong data-testid="policy-version">{usePolicyVersions(sheetId).length}</strong>
    </p>
  );
}

/** The policy versions, oldest first, once shown: "<version> <from or start> <key>=<value> <claims under it>". */
function PolicyViewer({ sheetId }: { sheetId: string }) {
  const [shown, setShown] = useState(false);
  const versions = usePolicyVersions(sheetId);
  const lines = usePolicyVersionLines(sheetId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const under = new Map<number, number>();
  for (const claim of claims) {
    const no = policyOn(versions, claim.date);
    under.set(no, (under.get(no) ?? 0) + 1);
  }
  return (
    <section data-testid="policy-viewer">
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ol data-testid="policy-lines">
          {lines.map((v, i) => (
            <li key={i} data-testid="policy-line">
              {`${i + 1} ${v.from ?? 'start'} ${v.key}=${v.value} ${under.get(i + 1) ?? 0}`}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

/** Downloads the policy versions as CSV, oldest first: each with its from-date (blank for none), setting and value, how
 *  many claims are under it (dated while it is in force) and the figures of those that count added up. The data is
 *  re-read first, so the file holds what the store holds now. */
function ExportPolicy({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const exportCsv = async () => {
    const fresh = { staleTime: 0 };
    const [settings, people, claims, rates] = await Promise.all([
      queryClient.fetchQuery({ ...settingsQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...peopleQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...claimsQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...ratesQuery(sheetId), ...fresh }),
    ]);
    const now = { ...late, people, claims };
    downloadCsv('policy.csv', [
      ['version', 'from', 'key', 'value', 'claims', 'due-total'],
      ...settings.policyLines.map((v, i) => {
        const under = claims.filter((c) => policyOn(settings.policyVersions, c.date) === i + 1);
        const dues = under
          .filter((c) => counts(c, people, late.today))
          .map((c) => figure(c, rates, people.find((p) => p.id === c.personId), floor, now));
        return [String(i + 1), v.from ?? '', v.key, v.value, String(under.length), sumDue(dues)];
      }),
    ]);
  };

  return (
    <p>
      <button type="button" data-testid="export-policy" onClick={() => void exportCsv()}>
        Export policy versions (CSV)
      </button>
    </p>
  );
}

/** The policy version in force on the claim's date. */
function ClaimPolicy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-policy">{policyOn(usePolicyVersions(sheetId), claim.date)}</td>;
}

export const contributions = [
  SheetPanel.fill({ order: 25.6, Component: PolicyVersion }),
  SheetPanel.fill({ order: 25.7, Component: PolicyViewer }),
  SheetPanel.fill({ order: 25.8, Component: ExportPolicy }),
  ClaimCells.fill({ order: 10.5, Component: ClaimPolicy }),
];
