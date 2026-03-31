import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";

const beforeEachSpy = vi.fn();
const useAuthStoreMock = vi.fn();
const messageErrorSpy = vi.fn();

vi.mock("./index", () => ({
  default: {
    beforeEach: beforeEachSpy,
  },
}));

vi.mock("../store/auth", () => ({
  useAuthStore: useAuthStoreMock,
}));

vi.mock("element-plus", () => ({
  ElMessage: {
    error: messageErrorSpy,
  },
}));

describe("router guards", () => {
  let guard: (to: any, from: any, next: (value?: unknown) => void) => Promise<void>;

  beforeAll(async () => {
    await import("./guards");
    const registration = beforeEachSpy.mock.calls[0];
    if (!registration) {
      throw new Error("router.beforeEach was not registered");
    }
    guard = registration[0] as typeof guard;
  });

  beforeEach(() => {
    document.title = "";
    useAuthStoreMock.mockReset();
    messageErrorSpy.mockReset();
  });

  it("redirects unauthenticated users to login", async () => {
    useAuthStoreMock.mockReturnValue({ isLoggedIn: false });
    const next = vi.fn();

    await guard({ meta: { requiresAuth: true }, fullPath: "/admin" }, {}, next);

    expect(next).toHaveBeenCalledWith({ name: "Login", query: { redirect: "/admin" } });
  });

  it("redirects to forbidden when role does not match", async () => {
    useAuthStoreMock.mockReturnValue({
      isLoggedIn: true,
      user: { username: "alice" },
      userRoles: ["CUSTOMER"],
    });
    const next = vi.fn();

    await guard(
      {
        meta: { requiresAuth: true, roles: ["ADMIN"], title: "管理页" },
        fullPath: "/admin",
      },
      {},
      next,
    );

    expect(document.title).toBe("管理页 - 美食速递");
    expect(next).toHaveBeenCalledWith({ name: "Forbidden" });
  });

  it("logs out and redirects when fetching user info fails", async () => {
    const logout = vi.fn();
    useAuthStoreMock.mockReturnValue({
      isLoggedIn: true,
      user: null,
      userRoles: ["CUSTOMER"],
      fetchUserInfo: vi.fn().mockRejectedValue(new Error("boom")),
      logout,
    });
    const next = vi.fn();

    await guard(
      {
        meta: { requiresAuth: true },
        fullPath: "/profile",
      },
      {},
      next,
    );

    expect(messageErrorSpy).toHaveBeenCalled();
    expect(logout).toHaveBeenCalled();
    expect(next).toHaveBeenCalledWith({ name: "Login", query: { redirect: "/profile" } });
  });

  it("passes through public routes", async () => {
    useAuthStoreMock.mockReturnValue({ isLoggedIn: false });
    const next = vi.fn();

    await guard({ meta: { title: "首页" }, fullPath: "/" }, {}, next);

    expect(document.title).toBe("首页 - 美食速递");
    expect(next).toHaveBeenCalledWith();
  });
});