import { beforeEach, describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import ServiceDegradeNotice from "./ServiceDegradeNotice.vue";
import { hideServiceDegrade, showServiceDegrade } from "../utils/serviceDegrade";

describe("ServiceDegradeNotice", () => {
  beforeEach(() => {
    hideServiceDegrade();
  });

  it("renders degrade information and hides on secondary action", async () => {
    showServiceDegrade({
      message: "网关异常",
      detail: "order-service 超时",
      statusCode: 503,
      requestUrl: "/api/orders",
    });

    const wrapper = mount(ServiceDegradeNotice, {
      global: {
        stubs: {
          transition: false,
        },
      },
    });

    expect(wrapper.text()).toContain("服务降级提示");
    expect(wrapper.text()).toContain("网关异常");
    expect(wrapper.text()).toContain("503");
    expect(wrapper.text()).toContain("/api/orders");

    await wrapper.get("button.secondary").trigger("click");
    expect(wrapper.html()).not.toContain("服务降级提示");
  });

  it("reloads page when primary action is clicked", async () => {
    showServiceDegrade({ message: "需要刷新" });
    const reloadSpy = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { reload: reloadSpy },
    });

    const wrapper = mount(ServiceDegradeNotice, {
      global: {
        stubs: {
          transition: false,
        },
      },
    });

    await wrapper.get("button.primary").trigger("click");
    expect(reloadSpy).toHaveBeenCalled();
  });
});