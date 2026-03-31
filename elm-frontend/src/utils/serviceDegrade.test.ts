import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  hideServiceDegrade,
  serviceDegradeState,
  showServiceDegrade,
} from "./serviceDegrade";

describe("serviceDegradeState", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-31T10:20:30+08:00"));
    hideServiceDegrade();
  });

  it("stores degrade details", () => {
    showServiceDegrade({
      message: "网关异常",
      detail: "order-service 超时",
      statusCode: 503,
      requestUrl: "/api/orders",
    });

    expect(serviceDegradeState.visible).toBe(true);
    expect(serviceDegradeState.message).toBe("网关异常");
    expect(serviceDegradeState.detail).toBe("order-service 超时");
    expect(serviceDegradeState.statusCode).toBe(503);
    expect(serviceDegradeState.requestUrl).toBe("/api/orders");
    expect(serviceDegradeState.occurredAt).toContain("2026/03/31");
  });

  it("hides the degrade banner without mutating message", () => {
    showServiceDegrade({ message: "临时失败" });
    hideServiceDegrade();

    expect(serviceDegradeState.visible).toBe(false);
    expect(serviceDegradeState.message).toBe("临时失败");
  });
});