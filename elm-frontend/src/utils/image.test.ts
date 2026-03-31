import { describe, expect, it } from "vitest";
import { formatBase64Image } from "./image";

describe("formatBase64Image", () => {
  it("returns empty string for invalid input", () => {
    expect(formatBase64Image(undefined)).toBe("");
    expect(formatBase64Image(null)).toBe("");
    expect(formatBase64Image("")).toBe("");
  });

  it("keeps existing data uri unchanged", () => {
    expect(formatBase64Image("data:image/png;base64,abc")).toBe(
      "data:image/png;base64,abc",
    );
  });

  it("detects common image mime types from base64 prefix", () => {
    expect(formatBase64Image("/9j/abcd")).toContain("data:image/jpeg;base64,/9j/abcd");
    expect(formatBase64Image("iVBORabcd")).toContain("data:image/png;base64,iVBORabcd");
    expect(formatBase64Image("R0lGODabcd")).toContain("data:image/gif;base64,R0lGODabcd");
    expect(formatBase64Image("UklGRabcd")).toContain("data:image/webp;base64,UklGRabcd");
    expect(formatBase64Image("PHN2Zyabcd")).toContain("data:image/svg+xml;base64,PHN2Zyabcd");
  });

  it("falls back to png for unknown data", () => {
    expect(formatBase64Image("unknown")).toBe("data:image/png;base64,unknown");
  });
});