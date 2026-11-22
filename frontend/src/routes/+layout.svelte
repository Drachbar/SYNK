<script lang="ts">
	import { resolve } from '$app/paths';
	import '../app.css';
	import favicon from '#lib/assets/favicon.svg';
	import { goto } from '$app/navigation';
	import { send } from '#lib/api.ts';
	import type { LayoutProps } from './$types';

	let { data, children }: LayoutProps = $props();

	async function logout() {
		await send('/api/auth/logout', { method: 'POST' });
		await goto(resolve('/login'), { invalidateAll: true });
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{#if data.user}
	<header>
		<a class="brand" href={resolve('/')}>SYNK</a>
		<nav>
			<a href={resolve('/veckor')}>Alla veckor</a>
			<span class="muted">{data.user.name}</span>
			<button onclick={logout}>Logga ut</button>
		</nav>
	</header>
{/if}

<main>
	{@render children()}
</main>

<style>
	header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.75rem 1rem;
		border-bottom: 1px solid var(--border);
		background: var(--surface);
	}

	.brand {
		font-weight: 800;
		letter-spacing: 0.08em;
		text-decoration: none;
		color: var(--text);
	}

	nav {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	nav button {
		padding: 0.3rem 0.75rem;
	}

	main {
		max-width: 46rem;
		margin: 0 auto;
		padding: 1.5rem 1rem 4rem;
	}
</style>
