import { error } from '@sveltejs/kit';
import type { EntrySummary } from '#lib/api.ts';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	const res = await fetch('/api/entries');
	if (!res.ok) error(res.status, 'Kunde inte hämta veckorna');
	return { entries: (await res.json()) as EntrySummary[] };
};
