<template>
  <div class="profile-layout">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// No script logic needed anymore, it's handled in the parent layout.
</script>

<style lang="scss" scoped>
.profile-layout {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
