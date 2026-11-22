import { redirect } from '@sveltejs/kit';
import { resolve } from '$app/paths';
import { weekOf, weekParams } from '#lib/week.ts';
import type { PageLoad } from './$types';

/** Startsidan är veckan vi är i nu. */
export const load: PageLoad = () => {
	redirect(307, resolve('/vecka/[year]/[week]', weekParams(weekOf(new Date()))));
};
