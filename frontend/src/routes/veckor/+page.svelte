<script lang="ts">
	import { resolve } from '$app/paths';
	import { formatTimestamp } from '#lib/api.ts';
	import { formatRange, weekParams } from '#lib/week.ts';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<h1>Alla veckor</h1>

{#if data.entries.length === 0}
	<p class="muted">Inga anteckningar än. <a href={resolve('/')}>Börja med denna vecka.</a></p>
{:else}
	<ul>
		{#each data.entries as entry (`${entry.isoYear}-${entry.isoWeek}`)}
			{@const week = { year: entry.isoYear, week: entry.isoWeek }}
			<li>
				<a href={resolve('/vecka/[year]/[week]', weekParams(week))}
					>Vecka {entry.isoWeek}, {entry.isoYear}</a
				>
				<span class="muted">· {formatRange(week)}</span>
				<div class="muted small">
					Senast sparad av {entry.updatedBy}, {formatTimestamp(entry.updatedAt)}
				</div>
			</li>
		{/each}
	</ul>
{/if}

<style>
	ul {
		list-style: none;
		padding: 0;
		display: grid;
		gap: 0.75rem;
	}

	li {
		background: var(--surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		padding: 0.75rem 1rem;
	}

	li a {
		font-weight: 600;
	}

	.small {
		font-size: 0.875rem;
	}
</style>
