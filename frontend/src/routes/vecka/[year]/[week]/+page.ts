import { error } from '@sveltejs/kit';
import type { EntryResponse } from '#lib/api.ts';
import type { Form, User } from '#lib/form/form.ts';
import { isValidWeek } from '#lib/week.ts';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params, fetch }) => {
	const week = { year: Number(params.year), week: Number(params.week) };
	if (!isValidWeek(week)) error(404, 'Den veckan finns inte');

	async function json<T>(path: string): Promise<T | null> {
		const res = await fetch(path);
		if (res.status === 204) return null;
		if (!res.ok) error(res.status, 'Kunde inte hämta veckan');
		return (await res.json()) as T;
	}

	const base = `/api/entries/${week.year}/${week.week}`;
	const [entry, users] = await Promise.all([json<EntryResponse>(base), json<User[]>('/api/users')]);

	// En anteckning visas med formuläret den sparades med. En ny vecka får det senaste
	// formuläret och förifylls från förra anteckningen.
	const [form, previous] = await Promise.all([
		json<Form>(entry ? `/api/forms/${entry.formVersion}` : '/api/forms/latest'),
		entry ? null : json<EntryResponse>(`${base}/previous`)
	]);

	return { week, entry, form: form!, users: users ?? [], previous };
};
