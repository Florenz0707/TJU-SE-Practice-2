import { beforeEach, describe, expect, it } from "vitest";
import { isMobile } from "./device";

describe("isMobile", () => {
  beforeEach(() => {
    Object.defineProperty(window, "innerWidth", {
      configurable: true,
      writable: true,
      value: 1280,
    });
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value: "Mozilla/5.0 (X11; Linux x86_64)",
    });
  });

  it("returns false for desktop agents on wide screens", () => {
    expect(isMobile()).toBe(false);
  });

  it("returns true for mobile user agents", () => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value: "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
    });

    expect(isMobile()).toBe(true);
  });

  it("returns true for narrow screens", () => {
    Object.defineProperty(window, "innerWidth", {
      configurable: true,
      writable: true,
      value: 375,
    });

    expect(isMobile()).toBe(true);
  });
});