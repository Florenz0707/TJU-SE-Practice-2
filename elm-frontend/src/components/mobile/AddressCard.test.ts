import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import AddressCard from "./AddressCard.vue";

describe("AddressCard", () => {
  it("renders address content and selected state", () => {
    const wrapper = mount(AddressCard, {
      props: {
        address: {
          contactName: "张三",
          contactTel: "18800000000",
          contactSex: 1,
          address: "天津大学北洋园校区",
        },
        selected: true,
      },
      global: {
        stubs: {
          CheckCircle2: true,
        },
      },
    });

    expect(wrapper.text()).toContain("张三");
    expect(wrapper.text()).toContain("18800000000");
    expect(wrapper.text()).toContain("天津大学北洋园校区");
    expect(wrapper.classes()).toContain("is-selected");
  });

  it("emits select when clicked", async () => {
    const wrapper = mount(AddressCard, {
      props: {
        address: {
          contactName: "李四",
          contactTel: "17700000000",
          contactSex: 0,
          address: "天津大学卫津路校区",
        },
        selected: false,
      },
      global: {
        stubs: {
          CheckCircle2: true,
        },
      },
    });

    await wrapper.trigger("click");
    expect(wrapper.emitted("select")).toHaveLength(1);
  });
});