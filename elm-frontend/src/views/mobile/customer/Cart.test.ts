import { defineComponent, h } from "vue";
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CartView from "./Cart.vue";

const testContext = vi.hoisted(() => ({
  pushSpy: vi.fn(),
  fetchCartSpy: vi.fn(),
  updateItemQuantitySpy: vi.fn(),
  removeItemSpy: vi.fn(),
  store: {
    loading: false,
    error: null as string | null,
    items: [] as Array<Record<string, unknown>>,
    itemsForCurrentBusiness: [] as Array<Record<string, unknown>>,
    cartTotal: 0,
    fetchCart: vi.fn(),
    updateItemQuantity: vi.fn(),
    removeItem: vi.fn(),
  },
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push: testContext.pushSpy,
  }),
}));

vi.mock("../../../store/cart", () => ({
  useCartStore: () => testContext.store,
}));

const ElButtonStub = defineComponent({
  name: "ElButton",
  props: {
    type: {
      type: String,
      default: undefined,
    },
    size: {
      type: String,
      default: undefined,
    },
    text: {
      type: Boolean,
      default: false,
    },
    plain: {
      type: Boolean,
      default: false,
    },
  },
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

const ElButtonGroupStub = defineComponent({
  name: "ElButtonGroup",
  setup(_, { slots }) {
    return () => h("div", slots.default ? slots.default() : []);
  },
});

const createCartItem = (quantity: number) => ({
  id: 101,
  quantity,
  food: {
    id: 5,
    foodName: "宫保鸡丁",
    foodPrice: 18,
  },
  business: {
    id: 9,
  },
});

const mountCart = () => {
  const router = { push: testContext.pushSpy };
  return mount(CartView, {
    global: {
      stubs: {
        ElButton: ElButtonStub,
        ElButtonGroup: ElButtonGroupStub,
      },
      mocks: {
        $router: router,
      },
    },
  });
};

describe("Mobile Cart view", () => {
  beforeEach(() => {
    testContext.pushSpy.mockReset();
    testContext.fetchCartSpy = vi.fn();
    testContext.updateItemQuantitySpy = vi.fn();
    testContext.removeItemSpy = vi.fn();
    testContext.store.loading = false;
    testContext.store.error = null;
    testContext.store.items = [];
    testContext.store.itemsForCurrentBusiness = [];
    testContext.store.cartTotal = 0;
    testContext.store.fetchCart = testContext.fetchCartSpy;
    testContext.store.updateItemQuantity = testContext.updateItemQuantitySpy;
    testContext.store.removeItem = testContext.removeItemSpy;
  });

  it("fetches cart on mount when local items are empty", () => {
    mountCart();

    expect(testContext.fetchCartSpy).toHaveBeenCalledTimes(1);
  });

  it("shows empty state and navigates back to mobile home", async () => {
    const wrapper = mountCart();

    expect(wrapper.text()).toContain("购物车是空的");

    await wrapper.get("button").trigger("click");

    expect(testContext.pushSpy).toHaveBeenCalledWith("/mobile/home");
  });

  it("renders items, updates quantity, removes item, and goes to checkout", async () => {
    testContext.store.items = [createCartItem(2)];
    testContext.store.itemsForCurrentBusiness = [createCartItem(2)];
    testContext.store.cartTotal = 36;

    const wrapper = mountCart();

    expect(testContext.fetchCartSpy).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("宫保鸡丁");
    expect(wrapper.text()).toContain("¥36.00");

    const buttons = wrapper.findAll("button");
    const decrementButton = buttons[0];
    const removeButton = buttons[3];
    const checkoutButton = buttons[4];

    if (!decrementButton || !removeButton || !checkoutButton) {
      throw new Error("Expected cart action buttons to be rendered");
    }

    await decrementButton.trigger("click");
    expect(testContext.updateItemQuantitySpy).toHaveBeenCalledWith(101, 1);

    await removeButton.trigger("click");
    expect(testContext.removeItemSpy).toHaveBeenCalledWith(101);

    await checkoutButton.trigger("click");
    expect(testContext.pushSpy).toHaveBeenCalledWith("/mobile/checkout");
  });
});