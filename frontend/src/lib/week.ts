// ISO 8601-veckor, samma som iso_year/iso_week i databasen (se docs/databas.md).
// Alla beräkningar görs i UTC så att sommartid inte flyttar dagar.

export type Week = { year: number; week: number };

const DAY = 24 * 60 * 60 * 1000;

/** Veckan som ett datum (lokal kalenderdag) tillhör. */
export function weekOf(date: Date): Week {
	const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
	return weekOfUtc(d);
}

function weekOfUtc(d: Date): Week {
	// Torsdagen i samma vecka avgör vilket år veckan tillhör.
	const thursday = new Date(d.getTime() + (3 - ((d.getUTCDay() + 6) % 7)) * DAY);
	const year = thursday.getUTCFullYear();
	const jan1 = Date.UTC(year, 0, 1);
	return { year, week: Math.floor((thursday.getTime() - jan1) / DAY / 7) + 1 };
}

/** Måndagen i veckan, som UTC-midnatt. */
export function mondayOf({ year, week }: Week): Date {
	// 4 januari ligger alltid i vecka 1.
	const jan4 = new Date(Date.UTC(year, 0, 4));
	const mondayWeek1 = jan4.getTime() - ((jan4.getUTCDay() + 6) % 7) * DAY;
	return new Date(mondayWeek1 + (week - 1) * 7 * DAY);
}

export function weeksInYear(year: number): number {
	// 28 december ligger alltid i årets sista vecka.
	return weekOfUtc(new Date(Date.UTC(year, 11, 28))).week;
}

export function isValidWeek({ year, week }: Week): boolean {
	return (
		Number.isInteger(year) &&
		Number.isInteger(week) &&
		year >= 2000 &&
		year <= 2999 &&
		week >= 1 &&
		week <= weeksInYear(year)
	);
}

export function addWeeks(w: Week, n: number): Week {
	return weekOfUtc(new Date(mondayOf(w).getTime() + n * 7 * DAY));
}

export function sameWeek(a: Week, b: Week): boolean {
	return a.year === b.year && a.week === b.week;
}

/** T.ex. "16–22 nov" eller "29 dec – 4 jan". */
export function formatRange(w: Week): string {
	const monday = mondayOf(w);
	const sunday = new Date(monday.getTime() + 6 * DAY);
	const day = (d: Date) => d.getUTCDate();
	const month = (d: Date) =>
		d.toLocaleDateString('sv-SE', { month: 'short', timeZone: 'UTC' }).replace('.', '');
	return month(monday) === month(sunday)
		? `${day(monday)}–${day(sunday)} ${month(sunday)}`
		: `${day(monday)} ${month(monday)} – ${day(sunday)} ${month(sunday)}`;
}

/** Parametrar till routen /vecka/[year]/[week]. */
export function weekParams({ year, week }: Week): { year: string; week: string } {
	return { year: String(year), week: String(week) };
}
