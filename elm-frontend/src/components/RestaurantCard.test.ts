import { describe, expect, it, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import RestaurantCard from "./RestaurantCard.vue";

const pushSpy = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push: pushSpy,
  }),
}));

describe("RestaurantCard", () => {
  beforeEach(() => {
    pushSpy.mockReset();
  });

  it("renders placeholder content when image is missing", () => {
    const wrapper = mount(RestaurantCard, {
      props: {
        business: {
          id: 7,
          businessName: "北洋食堂",
          businessAddress: "天津大学北洋园",
          startPrice: 20,
          deliveryPrice: 3,
        },
      },
    });

    expect(wrapper.text()).toContain("北洋食堂");
    expect(wrapper.text()).toContain("天津大学北洋园");
    expect(wrapper.text()).toContain("起送价: ¥20");
    expect(wrapper.find(".placeholder-image").exists()).toBe(true);
  });

  it("formats image source and navigates on click", async () => {
    const wrapper = mount(RestaurantCard, {
      props: {
        business: {
          id: 9,
          businessName: "学苑餐厅",
          businessAddress: "卫津路校区",
          startPrice: 15,
          deliveryPrice: 2,
          businessImg: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
        },
      },
    });

    expect(wrapper.get("img.restaurant-image").attributes("src")).toContain(
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
    );

    await wrapper.trigger("click");
    expect(pushSpy).toHaveBeenCalledWith({
      name: "RestaurantDetail",
      params: { id: 9 },
    });
  });
});