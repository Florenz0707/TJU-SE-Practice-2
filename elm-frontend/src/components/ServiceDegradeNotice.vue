<template>
  <transition name="degrade-fade">
    <div v-if="serviceDegradeState.visible" class="degrade-mask">
      <div class="degrade-panel">
        <div class="degrade-badge">服务降级提示</div>
        <h2>当前请求链路出现异常</h2>
        <p class="degrade-message">{{ serviceDegradeState.message }}</p>
        <dl class="degrade-meta">
          <div>
            <dt>触发时间</dt>
            <dd>{{ serviceDegradeState.occurredAt }}</dd>
          </div>
          <div v-if="serviceDegradeState.statusCode">
            <dt>状态码</dt>
            <dd>{{ serviceDegradeState.statusCode }}</dd>
          </div>
          <div v-if="serviceDegradeState.requestUrl">
            <dt>请求地址</dt>
            <dd>{{ serviceDegradeState.requestUrl }}</dd>
          </div>
        </dl>
        <p class="degrade-detail">{{ serviceDegradeState.detail }}</p>
        <div class="degrade-actions">
          <button type="button" class="secondary" @click="hideServiceDegrade">
            知道了
          </button>
          <button type="button" class="primary" @click="reloadPage">
            重新加载
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import {
  hideServiceDegrade,
  serviceDegradeState,
} from "../utils/serviceDegrade";

function reloadPage() {
  window.location.reload();
}
</script>

<style scoped>
.degrade-mask {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.08), transparent 35%),
    rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(10px);
}

.degrade-panel {
  width: min(100%, 560px);
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 24px;
  background: linear-gradient(160deg, #fffdf8 0%, #fff3e6 100%);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.28);
  color: #1f2937;
}

.degrade-badge {
  display: inline-flex;
  margin-bottom: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #1f2937;
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.degrade-panel h2 {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
}

.degrade-message {
  margin: 0 0 18px;
  font-size: 16px;
  color: #374151;
}

.degrade-meta {
  display: grid;
  gap: 10px;
  margin: 0 0 18px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
}

.degrade-meta div {
  display: grid;
  gap: 4px;
}

.degrade-meta dt {
  font-size: 12px;
  color: #6b7280;
}

.degrade-meta dd {
  margin: 0;
  font-size: 14px;
  word-break: break-all;
}

.degrade-detail {
  margin: 0 0 24px;
  color: #4b5563;
  line-height: 1.7;
}

.degrade-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.degrade-actions button {
  border: 0;
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  cursor: pointer;
}

.degrade-actions .secondary {
  background: rgba(31, 41, 55, 0.08);
  color: #1f2937;
}

.degrade-actions .primary {
  background: #d97706;
  color: #fff;
}

.degrade-fade-enter-active,
.degrade-fade-leave-active {
  transition: opacity 0.2s ease;
}

.degrade-fade-enter-from,
.degrade-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .degrade-panel {
    padding: 22px;
    border-radius: 20px;
  }

  .degrade-panel h2 {
    font-size: 22px;
  }

  .degrade-actions {
    flex-direction: column;
  }
}
</style>
