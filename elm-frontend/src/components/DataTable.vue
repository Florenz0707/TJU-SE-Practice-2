<template>
  <el-table :data="data" stripe style="width: 100%">
    <el-table-column
      v-for="column in columns"
      :key="column.prop"
      :prop="column.prop"
      :label="column.label"
      :width="column.width"
    />
    <el-table-column label="操作" v-if="$slots.actions">
      <template #default="scope">
        <slot name="actions" :row="scope.row"></slot>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { PropType } from 'vue';

interface Column {
  prop: string;
  label: string;
  width?: string | number;
}

defineProps({
  data: {
    type: Array as PropType<any[]>,
    required: true,
  },
  columns: {
    type: Array as PropType<Column[]>,
    required: true,
  },
});
</script>

<style scoped>
/* Scoped styles for the DataTable component */
</style>
