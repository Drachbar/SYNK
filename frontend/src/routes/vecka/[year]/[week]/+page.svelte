<script lang="ts">
	import { resolve } from '$app/paths';
	import EntryForm from '#lib/entry/EntryForm.svelte';
	import { addWeeks, formatRange, sameWeek, weekOf, weekParams } from '#lib/week.ts';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	const current = weekOf(new Date());
	const prev = $derived(addWeeks(data.week, -1));
	const next = $derived(addWeeks(data.week, 1));
	const isCurrent = $derived(sameWeek(data.week, current));
</script>

<svelte:head>
	<title>Vecka {data.week.week} – SYNK</title>
</svelte:head>

<div class="weeknav">
	<a href={resolve('/vecka/[year]/[week]', weekParams(prev))} aria-label="Föregående vecka"
		>← v.{prev.week}</a
	>
	<div class="title">
		<h1>Vecka {data.week.week}</h1>
		<div class="muted">{formatRange(data.week)} {data.week.year}</div>
	</div>
	<a href={resolve('/vecka/[year]/[week]', weekParams(next))} aria-label="Nästa vecka"
		>v.{next.week} →</a
	>
</div>

{#if !isCurrent}
	<p class="today">
		<a href={resolve('/vecka/[year]/[week]', weekParams(current))}
			>Gå till denna vecka (v.{current.week})</a
		>
	</p>
{/if}

<!-- Ny komponent per vecka och per sparad version, så att formuläret alltid börjar från det som hämtats. -->
{#key `${data.week.year}-${data.week.week}-${data.entry?.version}`}
	<EntryForm
		week={data.week}
		entry={data.entry}
		form={data.form}
		users={data.users}
		previous={data.previous}
	/>
{/key}

<style>
	.weeknav {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.weeknav a {
		text-decoration: none;
		white-space: nowrap;
		padding: 0.4rem 0.6rem;
	}

	.title {
		text-align: center;
	}

	h1 {
		margin: 0;
		font-size: 1.6rem;
	}

	.today {
		text-align: center;
		margin-top: -0.5rem;
	}
</style>
