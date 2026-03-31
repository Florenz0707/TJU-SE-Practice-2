import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "./auth";
import { login as apiLogin } from "../api/auth";
import { getActualUser } from "../api/user";
import { setRequestToken } from "../utils/request";

vi.mock("../api/auth", () => ({ login: vi.fn() }));
vi.mock("../api/user", () => ({ getActualUser: vi.fn() }));
vi.mock("../utils/request", () => ({ setRequestToken: vi.fn() }));

describe("useAuthStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("login stores tokens and user info", async () => {
    vi.mocked(apiLogin).mockResolvedValue({
      id_token: "token-1",
      refresh_token: "refresh-1",
    });
    vi.mocked(getActualUser).mockResolvedValue({
      success: true,
      code: "OK",
      data: {
        id: 1,
        username: "alice",
        authorities: [{ name: "USER" }],
      },
    });

    const authStore = useAuthStore();
    const roles = await authStore.login({ username: "alice", password: "123456" });

    expect(authStore.token).toBe("token-1");
    expect(authStore.refreshToken).toBe("refresh-1");
    expect(authStore.user?.username).toBe("alice");
    expect(roles).toEqual(["CUSTOMER"]);
    expect(localStorage.getItem("authToken")).toBe("token-1");
    expect(vi.mocked(setRequestToken)).toHaveBeenCalledWith("token-1");
  });

  it("fetchUserInfo failure triggers logout", async () => {
    vi.mocked(getActualUser).mockRejectedValue(new Error("boom"));

    const authStore = useAuthStore();
    authStore.token = "token-1";
    authStore.refreshToken = "refresh-1";
    authStore.user = { username: "alice" };

    await expect(authStore.fetchUserInfo()).rejects.toThrow("Failed to fetch user info");
    expect(authStore.token).toBeNull();
    expect(authStore.refreshToken).toBeNull();
    expect(authStore.user).toBeNull();
    expect(vi.mocked(setRequestToken)).toHaveBeenCalledWith(null);
  });

  it("maps backend authorities to frontend roles", () => {
    const authStore = useAuthStore();
    authStore.user = {
      username: "merchant",
      authorities: [{ name: "BUSINESS" }, { name: "ADMIN" }],
    };

    expect(authStore.userRoles).toEqual(["MERCHANT", "ADMIN"]);
  });
});