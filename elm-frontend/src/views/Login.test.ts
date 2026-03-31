import { defineComponent, h, nextTick } from "vue";
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Login from "./Login.vue";

const testContext = vi.hoisted(() => ({
  pushSpy: vi.fn(),
  loginSpy: vi.fn(),
  successSpy: vi.fn(),
  validateSpy: vi.fn(),
  routeState: {
    query: {} as Record<string, unknown>,
  },
  mobileState: {
    value: false,
  },
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push: testContext.pushSpy,
  }),
  useRoute: () => testContext.routeState,
}));

vi.mock("../store/auth", () => ({
  useAuthStore: () => ({
    login: testContext.loginSpy,
  }),
}));

vi.mock("../utils/device", () => ({
  isMobile: () => testContext.mobileState.value,
}));

vi.mock("element-plus", () => ({
  ElMessage: {
    success: testContext.successSpy,
  },
}));

const ElFormStub = defineComponent({
  name: "ElForm",
  setup(_, { slots, expose }) {
    expose({
      validate: testContext.validateSpy,
    });

    return () => h("form", slots.default ? slots.default() : []);
  },
});

const ElFormItemStub = defineComponent({
  name: "ElFormItem",
  setup(_, { slots }) {
    return () => h("div", slots.default ? slots.default() : []);
  },
});

const ElInputStub = defineComponent({
  name: "ElInput",
  props: {
    modelValue: {
      type: String,
      default: "",
    },
    type: {
      type: String,
      default: "text",
    },
    placeholder: {
      type: String,
      default: "",
    },
    size: {
      type: String,
      default: undefined,
    },
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    return () =>
      h("input", {
        value: props.modelValue,
        type: props.type,
        placeholder: props.placeholder,
        onInput: (event: Event) => {
          emit("update:modelValue", (event.target as HTMLInputElement).value);
        },
      });
  },
});

const ElButtonStub = defineComponent({
  name: "ElButton",
  emits: ["click"],
  setup(_, { slots, emit }) {
    return () =>
      h(
        "button",
        {
          type: "button",
          onClick: () => emit("click"),
        },
        slots.default ? slots.default() : [],
      );
  },
});

const ElLinkStub = defineComponent({
  name: "ElLink",
  setup(_, { slots }) {
    return () => h("span", slots.default ? slots.default() : []);
  },
});

const RouterLinkStub = defineComponent({
  name: "RouterLink",
  props: {
    to: {
      type: [String, Object],
      required: false,
      default: undefined,
    },
  },
  setup(_, { slots }) {
    return () => h("a", slots.default ? slots.default() : []);
  },
});

const mountLogin = () =>
  mount(Login, {
    global: {
      stubs: {
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElButton: ElButtonStub,
        ElLink: ElLinkStub,
        RouterLink: RouterLinkStub,
      },
    },
  });

describe("Login view", () => {
  beforeEach(() => {
    testContext.pushSpy.mockReset();
    testContext.loginSpy.mockReset();
    testContext.successSpy.mockReset();
    testContext.validateSpy.mockReset();
    testContext.validateSpy.mockResolvedValue(undefined);
    testContext.routeState.query = {};
    testContext.mobileState.value = false;
  });

  it("redirects to route query after successful login", async () => {
    testContext.routeState.query = { redirect: "/checkout" };
    testContext.loginSpy.mockResolvedValue(["CUSTOMER"]);

    const wrapper = mountLogin();
    const usernameInput = wrapper.get('input[placeholder="请输入用户名"]');
    const passwordInput = wrapper.get('input[placeholder="请输入密码"]');

    await usernameInput.setValue("alice");
    await passwordInput.setValue("123456");
    await wrapper.get("button").trigger("click");
    await nextTick();

    expect(testContext.validateSpy).toHaveBeenCalledTimes(1);
    expect(testContext.loginSpy).toHaveBeenCalledWith({ username: "alice", password: "123456" });
    expect(testContext.successSpy).toHaveBeenCalledWith("登录成功！");
    expect(testContext.pushSpy).toHaveBeenCalledWith("/checkout");
  });

  it("routes admin users to mobile admin dashboard when no redirect is provided", async () => {
    testContext.mobileState.value = true;
    testContext.loginSpy.mockResolvedValue(["ADMIN"]);

    const wrapper = mountLogin();
    const usernameInput = wrapper.get('input[placeholder="请输入用户名"]');
    const passwordInput = wrapper.get('input[placeholder="请输入密码"]');

    await usernameInput.setValue("admin");
    await passwordInput.setValue("123456");
    await wrapper.get("button").trigger("click");
    await nextTick();

    expect(testContext.pushSpy).toHaveBeenCalledWith("/mobile/admin");
  });

  it("does not navigate when login fails", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    testContext.loginSpy.mockRejectedValue(new Error("bad credentials"));

    const wrapper = mountLogin();
    const usernameInput = wrapper.get('input[placeholder="请输入用户名"]');
    const passwordInput = wrapper.get('input[placeholder="请输入密码"]');

    await usernameInput.setValue("alice");
    await passwordInput.setValue("wrong-password");
    await wrapper.get("button").trigger("click");
    await nextTick();

    expect(testContext.pushSpy).not.toHaveBeenCalled();
    expect(testContext.successSpy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalled();

    errorSpy.mockRestore();
  });
});