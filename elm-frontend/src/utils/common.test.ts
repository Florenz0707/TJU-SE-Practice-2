import { describe, expect, it } from "vitest";
import { buildQueryString, formatDateTime } from "./common";

describe("buildQueryString", () => {
  it("skips empty values and repeats array entries", () => {
    expect(
      buildQueryString({
        keyword: "noodle",
        page: 2,
        empty: "",
        archived: false,
        tags: ["hot", "new"],
        ignored: null,
      }),
    ).toBe("keyword=noodle&page=2&archived=false&tags=hot&tags=new");
  });
});

describe("formatDateTime", () => {
  it("formats a date in zh-CN locale", () => {
    expect(formatDateTime(new Date("2026-03-31T10:20:30+08:00"))).toContain(
      "2026/03/31",
    );
  });
});