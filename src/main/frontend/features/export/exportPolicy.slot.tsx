import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, useClaims } from '../claims/useClaims';
import { gradeOn, usePeople } from '../people/usePeople';
import { claimRateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate, useSettings } from '../settings/useSettings';
import { downloadCsv, useSheetRead } from './csv';

/**
 * Downloads the policy versions as a CSV file: a header line `version,from,key,value,claims,due-total`,
 * then one line per version, oldest first, with its from-date (empty for none), the key and value it
 * set, the count of claims whose policy version it is (the one in force on the claim's date; DELETED
 * claims left out) and the due of those claims as their rows show it, added up (n/a skipped). Quoted
 * and ended as the people export.
 */
function ExportPolicy({ sheet }: { user: string; sheet: string }) {
  const versions = useSettings(sheet).data?.policyVersionList ?? [];
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);

  const read = useSheetRead(sheet);

  const download = () => {
    const versionOn = (date: string) => versions.filter((v) => v.from === null || v.from <= date).length;
    const lines = [['version', 'from', 'key', 'value', 'claims', 'due-total']];
    versions.forEach((v, i) => {
      const counted = claims.filter((c) => c.status !== 'DELETED' && versionOn(c.date) === i + 1);
      const dues = counted.map((c) => {
        const person = people.find((p) => p.id === c.personId);
        return person ? claimDueCents(c, floor, claimRateOn(rates, gradeOn(person, c.date), c), lateBy) : null;
      });
      lines.push([String(v.version), v.from ?? '', v.key, v.value, String(counted.length), totalDue(dues)]);
    });
    downloadCsv(lines, `policy-${sheet}.csv`);
  };

  return (
    <p>
      <button type="button" data-testid="export-policy" disabled={!read} onClick={download}>
        Export policy versions (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12.65, Component: ExportPolicy });
