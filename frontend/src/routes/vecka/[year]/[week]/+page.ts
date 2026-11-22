import { error } from '@sveltejs/kit';
import type { EntryResponse } from '#lib/api.ts';
import { isValidWeek } from '#lib/week.ts';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params, fetch }) => {
	const week = { year: Number(params.year), week: Number(params.week) };
	if (!isValidWeek(week)) error(404, 'Den veckan finns inte');

	const res = await fetch(`/api/entries/${week.year}/${week.week}`);
	if (res.status === 204) return { week, entry: null }; // veckan har ingen anteckning än
	if (!res.ok) error(res.status, 'Kunde inte hämta anteckningen');
	return { week, entry: (await res.json()) as EntryResponse };
};
