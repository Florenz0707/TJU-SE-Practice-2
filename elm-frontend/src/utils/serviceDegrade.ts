import { reactive, readonly } from "vue";
import { formatDateTime } from "./common";

interface ServiceDegradeSnapshot {
  visible: boolean;
  message: string;
  detail: string;
  occurredAt: string;
  statusCode: number | null;
  requestUrl: string;
}

const state = reactive<ServiceDegradeSnapshot>({
  visible: false,
  message: "",
  detail: "",
  occurredAt: "",
  statusCode: null,
  requestUrl: "",
});

export const serviceDegradeState = readonly(state);

export function showServiceDegrade(options: {
  message?: string;
  detail?: string;
  statusCode?: number | null;
  requestUrl?: string;
}) {
  state.visible = true;
  state.message = options.message || "服务暂时不可用，请稍后重试。";
  state.detail = options.detail || "网关或下游服务返回异常，前端已进入降级提示模式。";
  state.occurredAt = formatDateTime();
  state.statusCode = options.statusCode ?? null;
  state.requestUrl = options.requestUrl || "";
}

export function hideServiceDegrade() {
  state.visible = false;
}
