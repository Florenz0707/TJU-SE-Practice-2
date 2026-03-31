import { defineComponent, h, nextTick } from "vue";
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RestaurantDetailView from "./RestaurantDetail.vue";

const testContext = vi.hoisted(() => ({
  routeState: {
    params: { id: "1" },
  },
  getBusinessByIdSpy: vi.fn(),
  getAllFoodsSpy: vi.fn(),
  getBusinessReviewsSpy: vi.fn(),
  setCurrentBusinessIdSpy: vi.fn(),
  setBusinessFeesSpy: vi.fn(),
  addItemSpy: vi.fn(),
  messageSuccessSpy: vi.fn(),
  consoleErrorSpy: vi.fn(),
  cartStore: {
    setCurrentBusinessId: vi.fn(),
    setBusinessFees: vi.fn(),
    addItem: vi.fn(),
  },
}));

vi.mock("vue-router", () => ({
  useRoute: () => testContext.routeState,
}));

vi.mock("../../api/business", () => ({
  getBusinessById: testContext.getBusinessByIdSpy,
}));

vi.mock("../../api/food", () => ({
  getAllFoods: testContext.getAllFoodsSpy,
}));

vi.mock("../../api/review", () => ({
  getBusinessReviews: testContext.getBusinessReviewsSpy,
}));

vi.mock("../../store/cart", () => ({
  useCartStore: () => testContext.cartStore,
}));

vi.mock("element-plus", () => ({
  ElMessage: {
    success: testContext.messageSuccessSpy,
  },
}));

const passthroughStub = (name: string, tag = "div") =>
  defineComponent({
    name,
    props: {
      modelValue: {
        type: [String, Number, Boolean, Object, Array, null],
        default: undefined,
      },
      src: {
        type: String,
        default: undefined,
      },
    },
    setup(_, { slots }) {
      return () => h(tag, slots.default ? slots.default() : []);
    },
  });

const ElCardStub = defineComponent({
  name: "ElCard",
  setup(_, { slots }) {
    return () =>
      h("div", [
        slots.header ? slots.header() : null,
        slots.default ? slots.default() : null,
      ]);
  },
});

const ElButtonStub = defineComponent({
  name: "ElButton",
  setup(_, { attrs, slots }) {
    return () =>
      h(
        "button",
        {
          ...attrs,
          onClick: attrs.onClick as (() => void) | undefined,
        },
        slots.default ? slots.default() : [],
      );
  },
});

const mountRestaurantDetail = async () => {
  const wrapper = mount(RestaurantDetailView, {
    global: {
      stubs: {
        ElCard: ElCardStub,
        ElButton: ElButtonStub,
        ElImage: passthroughStub("ElImage"),
        ElRate: passthroughStub("ElRate"),
      },
    },
  });

  await Promise.resolve();
  await nextTick();
  await Promise.resolve();
  await nextTick();
  return wrapper;
};

describe("RestaurantDetail view", () => {
  beforeEach(() => {
    testContext.routeState.params = { id: "1" };
    testContext.getBusinessByIdSpy.mockReset();
    testContext.getAllFoodsSpy.mockReset();
    testContext.getBusinessReviewsSpy.mockReset();
    testContext.setCurrentBusinessIdSpy = vi.fn();
    testContext.setBusinessFeesSpy = vi.fn();
    testContext.addItemSpy = vi.fn();
    testContext.messageSuccessSpy.mockReset();
    testContext.cartStore.setCurrentBusinessId = testContext.setCurrentBusinessIdSpy;
    testContext.cartStore.setBusinessFees = testContext.setBusinessFeesSpy;
    testContext.cartStore.addItem = testContext.addItemSpy;
    testContext.consoleErrorSpy = vi.fn();

    testContext.getBusinessByIdSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: {
        id: 1,
        businessName: "北洋食堂",
        businessAddress: "天津大学北洋园",
        businessExplain: "招牌快餐",
        deliveryPrice: 4,
        startPrice: 20,
      },
    });
    testContext.getAllFoodsSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 5,
          foodName: "宫保鸡丁",
          foodPrice: 18,
          business: { id: 1, businessName: "北洋食堂" },
        },
      ],
    });
    testContext.getBusinessReviewsSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 9,
          stars: 5,
          content: "很好吃",
          anonymous: false,
          customer: { username: "alice" },
        },
      ],
    });
  });

  it("loads business, menu, and reviews while configuring cart context", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const wrapper = await mountRestaurantDetail();

    expect(testContext.setCurrentBusinessIdSpy).toHaveBeenCalledWith(1);
    expect(testContext.getBusinessByIdSpy).toHaveBeenCalledWith(1);
    expect(testContext.getAllFoodsSpy).toHaveBeenCalledWith({ business: 1 });
    expect(testContext.getBusinessReviewsSpy).toHaveBeenCalledWith(1);
    expect(testContext.setBusinessFeesSpy).toHaveBeenCalledWith(4, 20);
    expect(wrapper.text()).toContain("北洋食堂");
    expect(wrapper.text()).toContain("天津大学北洋园");
    expect(wrapper.text()).toContain("招牌快餐");
    expect(wrapper.text()).toContain("宫保鸡丁");
    expect(wrapper.text()).toContain("很好吃");
    expect(errorSpy).not.toHaveBeenCalled();

    errorSpy.mockRestore();
  });

  it("shows invalid id error and skips downstream requests", async () => {
    testContext.routeState.params = { id: "not-a-number" };

    const wrapper = await mountRestaurantDetail();

    expect(testContext.setCurrentBusinessIdSpy).toHaveBeenCalledWith(Number.NaN);
    expect(testContext.getBusinessByIdSpy).not.toHaveBeenCalled();
    expect(testContext.getAllFoodsSpy).not.toHaveBeenCalled();
    expect(testContext.getBusinessReviewsSpy).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("无效的餐厅ID。");
  });

  it("adds a menu item to cart when clicking the add button", async () => {
    const wrapper = await mountRestaurantDetail();

    await wrapper.get("button").trigger("click");

    expect(testContext.addItemSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 5,
        foodName: "宫保鸡丁",
      }),
      1,
    );
    expect(testContext.messageSuccessSpy).toHaveBeenCalledWith({
      message: "宫保鸡丁 已添加到购物车！",
      duration: 1500,
    });
  });
});