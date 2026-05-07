<script setup>
defineProps({
  metrics: {
    type: Array,
    required: true,
  },
})
</script>

<template>
  <section class="metric-strip">
    <article v-for="metric in metrics" :key="metric.label" class="metric-card">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
    </article>
  </section>
</template>
