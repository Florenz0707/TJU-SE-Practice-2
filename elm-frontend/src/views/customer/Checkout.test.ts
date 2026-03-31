import { defineComponent, h, nextTick } from "vue";
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CheckoutView from "./Checkout.vue";

const testContext = vi.hoisted(() => ({
  pushSpy: vi.fn(),
  warningSpy: vi.fn(),
  errorSpy: vi.fn(),
  successSpy: vi.fn(),
  confirmSpy: vi.fn(),
  getCurrentUserAddressesSpy: vi.fn(),
  addDeliveryAddressSpy: vi.fn(),
  updateDeliveryAddressSpy: vi.fn(),
  deleteDeliveryAddressSpy: vi.fn(),
  getMyVouchersSpy: vi.fn(),
  getMyPointsAccountSpy: vi.fn(),
  addOrderSpy: vi.fn(),
  walletFetchMyWalletSpy: vi.fn(),
  cartFetchCartSpy: vi.fn(),
  authUser: { id: 7, username: "alice" },
  cartStore: {
    itemsForCurrentBusiness: [] as Array<Record<string, unknown>>,
    cartTotal: 0,
    deliveryPrice: 0,
    finalOrderTotal: 0,
    fetchCart: vi.fn(),
  },
  walletStore: {
    walletBalance: 0,
    fetchMyWallet: vi.fn(),
  },
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push: testContext.pushSpy,
  }),
}));

vi.mock("../../store/cart", () => ({
  useCartStore: () => testContext.cartStore,
}));

vi.mock("../../store/auth", () => ({
  useAuthStore: () => ({
    user: testContext.authUser,
  }),
}));

vi.mock("../../store/wallet", () => ({
  useWalletStore: () => testContext.walletStore,
}));

vi.mock("../../api/address", () => ({
  getCurrentUserAddresses: testContext.getCurrentUserAddressesSpy,
  addDeliveryAddress: testContext.addDeliveryAddressSpy,
  updateDeliveryAddress: testContext.updateDeliveryAddressSpy,
  deleteDeliveryAddress: testContext.deleteDeliveryAddressSpy,
}));

vi.mock("../../api/privateVoucher", () => ({
  getMyVouchers: testContext.getMyVouchersSpy,
}));

vi.mock("../../api/points", () => ({
  getMyPointsAccount: testContext.getMyPointsAccountSpy,
}));

vi.mock("../../api/order", () => ({
  addOrder: testContext.addOrderSpy,
}));

vi.mock("element-plus", () => ({
  ElMessage: {
    warning: testContext.warningSpy,
    error: testContext.errorSpy,
    success: testContext.successSpy,
  },
  ElMessageBox: {
    confirm: testContext.confirmSpy,
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
      label: {
        type: [String, Number, Boolean, Object, null],
        default: undefined,
      },
      title: {
        type: String,
        default: undefined,
      },
      active: {
        type: Number,
        default: undefined,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    emits: ["click", "change", "update:modelValue"],
    setup(props, { slots, emit }) {
      return () =>
        h(
          tag,
          {
            "data-label": props.label,
            "data-title": props.title,
            "data-active": props.active,
            disabled: props.disabled || undefined,
            onClick: () => emit("click"),
            onChange: () => emit("change"),
          },
          slots.default ? slots.default() : [],
        );
    },
  });

const ElButtonStub = defineComponent({
  name: "ElButton",
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    type: {
      type: String,
      default: undefined,
    },
    size: {
      type: String,
      default: undefined,
    },
  },
  emits: ["click"],
  setup(props, { slots, emit }) {
    return () =>
      h(
        "button",
        {
          type: "button",
          disabled: props.disabled,
          onClick: () => {
            if (!props.disabled) {
              emit("click");
            }
          },
        },
        slots.default ? slots.default() : [],
      );
  },
});

const ElTableStub = defineComponent({
  name: "ElTable",
  setup(_, { slots }) {
    return () => h("div", slots.default ? slots.default() : []);
  },
});

const ElTableColumnStub = defineComponent({
  name: "ElTableColumn",
  setup() {
    return () => null;
  },
});

const mountCheckout = async () => {
  const wrapper = mount(CheckoutView, {
    global: {
      stubs: {
        ElSteps: passthroughStub("ElSteps"),
        ElStep: passthroughStub("ElStep"),
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElButton: ElButtonStub,
        ElRadioGroup: passthroughStub("ElRadioGroup"),
        ElRadioButton: passthroughStub("ElRadioButton"),
        ElSelect: passthroughStub("ElSelect"),
        ElOption: passthroughStub("ElOption"),
        ElTag: passthroughStub("ElTag", "span"),
        ElCheckbox: passthroughStub("ElCheckbox", "label"),
        ElInputNumber: passthroughStub("ElInputNumber"),
        ElDivider: passthroughStub("ElDivider", "hr"),
        ElDialog: passthroughStub("ElDialog"),
        ElForm: passthroughStub("ElForm", "form"),
        ElFormItem: passthroughStub("ElFormItem"),
        ElInput: passthroughStub("ElInput", "input"),
        ElRadio: passthroughStub("ElRadio", "label"),
      },
    },
  });

  await nextTick();
  await Promise.resolve();
  await nextTick();
  return wrapper;
};

const flushAsync = async () => {
  await Promise.resolve();
  await nextTick();
};

const setSetupStateValue = (wrapper: ReturnType<typeof mount>, key: string, value: unknown) => {
  const setupState = (wrapper.vm as any).$?.setupState as Record<string, unknown> | undefined;
  if (!setupState) {
    throw new Error("Checkout setup state is unavailable");
  }

  const current = setupState[key];
  if (current && typeof current === "object" && "value" in (current as Record<string, unknown>)) {
    (current as { value: unknown }).value = value;
    return;
  }

  setupState[key] = value;
};

const getSetupState = (wrapper: ReturnType<typeof mount>) => {
  const setupState = (wrapper.vm as any).$?.setupState as Record<string, unknown> | undefined;
  if (!setupState) {
    throw new Error("Checkout setup state is unavailable");
  }
  return setupState;
};

const getSetupStateBoolean = (wrapper: ReturnType<typeof mount>, key: string) => {
  const value = getSetupState(wrapper)[key];
  if (typeof value === "boolean") {
    return value;
  }
  if (value && typeof value === "object" && "value" in (value as Record<string, unknown>)) {
    return Boolean((value as { value: unknown }).value);
  }
  return Boolean(value);
};

const getSetupStateNumber = (wrapper: ReturnType<typeof mount>, key: string) => {
  const value = getSetupState(wrapper)[key];
  if (typeof value === "number") {
    return value;
  }
  if (value && typeof value === "object" && "value" in (value as Record<string, unknown>)) {
    return Number((value as { value: unknown }).value);
  }
  return Number(value);
};

const getSetupStateArray = (wrapper: ReturnType<typeof mount>, key: string) => {
  const value = getSetupState(wrapper)[key];
  if (Array.isArray(value)) {
    return value;
  }
  if (value && typeof value === "object" && "value" in (value as Record<string, unknown>)) {
    const innerValue = (value as { value: unknown }).value;
    return Array.isArray(innerValue) ? innerValue : [];
  }
  return [];
};

describe("Checkout view", () => {
  beforeEach(() => {
    testContext.pushSpy.mockReset();
    testContext.warningSpy.mockReset();
    testContext.errorSpy.mockReset();
    testContext.successSpy.mockReset();
    testContext.confirmSpy.mockReset();
    testContext.getCurrentUserAddressesSpy.mockReset();
    testContext.addDeliveryAddressSpy.mockReset();
    testContext.updateDeliveryAddressSpy.mockReset();
    testContext.deleteDeliveryAddressSpy.mockReset();
    testContext.getMyVouchersSpy.mockReset();
    testContext.getMyPointsAccountSpy.mockReset();
    testContext.addOrderSpy.mockReset();
    testContext.walletFetchMyWalletSpy = vi.fn().mockResolvedValue(undefined);
    testContext.cartFetchCartSpy = vi.fn().mockResolvedValue(undefined);
    testContext.walletStore.fetchMyWallet = testContext.walletFetchMyWalletSpy;
    testContext.walletStore.walletBalance = 80;
    testContext.cartStore.fetchCart = testContext.cartFetchCartSpy;
    testContext.cartStore.itemsForCurrentBusiness = [];
    testContext.cartStore.cartTotal = 0;
    testContext.cartStore.deliveryPrice = 0;
    testContext.cartStore.finalOrderTotal = 0;

    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [],
    });
    testContext.addDeliveryAddressSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: {},
    });
    testContext.updateDeliveryAddressSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: {},
    });
    testContext.deleteDeliveryAddressSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: {},
    });
    testContext.confirmSpy.mockResolvedValue(undefined);
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [],
    });
    testContext.getMyPointsAccountSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: {
        id: 1,
        userId: 7,
        totalPoints: 600,
        frozenPoints: 100,
        availablePoints: 500,
      },
    });
  });

  it("warns and redirects home when cart is empty", async () => {
    await mountCheckout();

    expect(testContext.warningSpy).toHaveBeenCalledWith("您的购物车是空的，正在跳转到主页。");
    expect(testContext.pushSpy).toHaveBeenCalledWith({ name: "Home" });
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(1);
    expect(testContext.getMyVouchersSpy).toHaveBeenCalledTimes(1);
    expect(testContext.getMyPointsAccountSpy).toHaveBeenCalledTimes(1);
    expect(testContext.walletFetchMyWalletSpy).toHaveBeenCalledTimes(1);
  });

  it("loads checkout data and renders order summary when cart has items", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 101,
          contactName: "张三",
          contactTel: "18800000000",
          contactSex: 1,
          address: "天津大学北洋园校区",
        },
      ],
    });
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: {
            id: 9,
            threshold: 30,
            value: 5,
            claimable: true,
            validDays: 30,
          },
        },
      ],
    });

    const wrapper = await mountCheckout();

    expect(testContext.warningSpy).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("确认您的订单");
    expect(wrapper.text()).toContain("商品总价: ¥36.00");
    expect(wrapper.text()).toContain("配送费: ¥4.00");
    expect(wrapper.text()).toContain("总计: ¥40.00");
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(1);
    expect(testContext.getMyVouchersSpy).toHaveBeenCalledTimes(1);
    expect(testContext.getMyPointsAccountSpy).toHaveBeenCalledTimes(1);
    expect(testContext.walletFetchMyWalletSpy).toHaveBeenCalledTimes(1);
  });

  it("places order successfully and redirects to order history", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });
    testContext.addOrderSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: { id: 501 },
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.addOrderSpy).toHaveBeenCalledWith({
      customer: testContext.authUser,
      business: { id: 9, businessName: "北洋食堂" },
      orderTotal: 40,
      deliveryAddress: address,
      orderState: 1,
      usedVoucher: undefined,
      voucherDiscount: 0,
      pointsUsed: 0,
      pointsDiscount: 0,
      walletPaid: 0,
      paymentMethod: "external",
    });
    expect(testContext.successSpy).toHaveBeenCalledWith("下单成功！");
    expect(testContext.cartFetchCartSpy).toHaveBeenCalledTimes(1);
    expect(testContext.walletFetchMyWalletSpy).toHaveBeenCalledTimes(2);
    expect(testContext.pushSpy).toHaveBeenCalledWith({ name: "OrderHistory" });
  });

  it("blocks order placement when wallet balance is insufficient", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.walletStore.walletBalance = 10;
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "paymentMethod", "wallet");
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.errorSpy).toHaveBeenCalledWith("钱包余额不足。");
    expect(testContext.addOrderSpy).not.toHaveBeenCalled();
    expect(testContext.pushSpy).not.toHaveBeenCalledWith({ name: "OrderHistory" });
  });

  it("blocks order placement when no address is selected", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", null);
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.errorSpy).toHaveBeenCalledWith("请选择配送地址。");
    expect(testContext.addOrderSpy).not.toHaveBeenCalled();
  });

  it("blocks order placement when cart item data is incomplete", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.errorSpy).toHaveBeenCalledWith("由于购物车信息不完整，无法下单。");
    expect(testContext.addOrderSpy).not.toHaveBeenCalled();
  });

  it("shows an error when order creation fails", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });
    testContext.addOrderSpy.mockResolvedValue({
      success: false,
      code: "ERR",
      message: "订单创建失败",
      data: null,
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.addOrderSpy).toHaveBeenCalledTimes(1);
    expect(testContext.errorSpy).toHaveBeenCalledWith("订单创建失败");
    expect(testContext.successSpy).not.toHaveBeenCalledWith("下单成功！");
    expect(testContext.pushSpy).not.toHaveBeenCalledWith({ name: "OrderHistory" });
  });

  it("resets used points when disabling points deduction", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "usePoints", true);
    setSetupStateValue(wrapper, "pointsToUse", 300);
    await flushAsync();

    setSetupStateValue(wrapper, "usePoints", false);
    await flushAsync();

    expect(getSetupStateBoolean(wrapper, "usePoints")).toBe(false);
    expect(getSetupStateNumber(wrapper, "pointsToUse")).toBe(0);
  });

  it("clips points to the allowed maximum when discounts reduce the payable amount", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: {
            id: 9,
            threshold: 30,
            value: 5,
            claimable: true,
            validDays: 30,
          },
        },
      ],
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "pointsAccount", {
      id: 1,
      userId: 7,
      totalPoints: 6000,
      frozenPoints: 0,
      availablePoints: 6000,
    });
    setSetupStateValue(wrapper, "usePoints", true);
    setSetupStateValue(wrapper, "pointsToUse", 4000);
    await flushAsync();

    setSetupStateValue(wrapper, "selectedVoucherId", 201);
    await flushAsync();

    expect(getSetupStateNumber(wrapper, "pointsToUse")).toBe(3500);
  });

  it("keeps only active vouchers after loading checkout data", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: { id: 9, threshold: 30, value: 5, claimable: true, validDays: 30 },
        },
        {
          id: 202,
          walletId: 1,
          publicVoucherId: 10,
          value: 8,
          threshold: 40,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: true,
          publicVoucher: { id: 10, threshold: 40, value: 8, claimable: true, validDays: 30 },
        },
        {
          id: 203,
          walletId: 1,
          publicVoucherId: 11,
          value: 3,
          threshold: 20,
          expiryDate: "2020-01-01T00:00:00.000Z",
          used: false,
          publicVoucher: { id: 11, threshold: 20, value: 3, claimable: true, validDays: 30 },
        },
      ],
    });

    const wrapper = await mountCheckout();

    const availableVouchers = getSetupStateArray(wrapper, "availableVouchers") as Array<{ id: number }>;

    expect(availableVouchers.map((voucher) => voucher.id)).toEqual([201]);
  });

  it("does not apply voucher discount when the order total does not meet the threshold", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 16;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 20;
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: { id: 9, threshold: 30, value: 5, claimable: true, validDays: 30 },
        },
      ],
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedVoucherId", 201);
    await flushAsync();

    expect(getSetupStateNumber(wrapper, "voucherDiscount")).toBe(0);
    expect(getSetupStateNumber(wrapper, "finalPrice")).toBe(20);
  });

  it("shows an error when voucher loading fails", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: false,
      code: "ERR",
      message: "优惠券服务异常",
      data: [],
    });

    await mountCheckout();

    expect(testContext.errorSpy).toHaveBeenCalledWith("优惠券服务异常");
  });

  it("shows an error when points account loading fails", async () => {
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getMyPointsAccountSpy.mockResolvedValue({
      success: false,
      code: "ERR",
      message: "积分账户加载失败",
      data: null,
    });

    await mountCheckout();

    expect(testContext.errorSpy).toHaveBeenCalledWith("积分账户加载失败");
  });

  it("updates wallet payment amount when payment method changes", async () => {
    testContext.walletStore.walletBalance = 15;
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 1,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;

    const wrapper = await mountCheckout();
    const setupState = getSetupState(wrapper);

    setSetupStateValue(wrapper, "paymentMethod", "wallet");
    (setupState.onPaymentMethodChange as () => void)();
    await flushAsync();
    expect(getSetupStateNumber(wrapper, "walletPayAmount")).toBe(40);

    setSetupStateValue(wrapper, "paymentMethod", "external");
    (setupState.onPaymentMethodChange as () => void)();
    await flushAsync();
    expect(getSetupStateNumber(wrapper, "walletPayAmount")).toBe(0);

    setSetupStateValue(wrapper, "paymentMethod", "mixed");
    (setupState.onPaymentMethodChange as () => void)();
    await flushAsync();
    expect(getSetupStateNumber(wrapper, "walletPayAmount")).toBe(15);
  });

  it("recomputes mixed payment split after voucher and points discounts", async () => {
    testContext.walletStore.walletBalance = 20;
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: { id: 9, threshold: 30, value: 5, claimable: true, validDays: 30 },
        },
      ],
    });

    const wrapper = await mountCheckout();
    const setupState = getSetupState(wrapper);

    setSetupStateValue(wrapper, "selectedVoucherId", 201);
    setSetupStateValue(wrapper, "usePoints", true);
    setSetupStateValue(wrapper, "pointsToUse", 300);
    setSetupStateValue(wrapper, "paymentMethod", "mixed");
    (setupState.onPaymentMethodChange as () => void)();
    await flushAsync();

    expect(getSetupStateNumber(wrapper, "walletPayAmount")).toBe(20);
    expect(getSetupStateNumber(wrapper, "externalPayAmount")).toBe(12);
    expect(getSetupStateNumber(wrapper, "finalPrice")).toBe(32);
  });

  it("uses wallet payment details in order payload", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.walletStore.walletBalance = 80;
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });
    testContext.addOrderSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: { id: 601 },
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "paymentMethod", "wallet");
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.addOrderSpy).toHaveBeenCalledWith({
      customer: testContext.authUser,
      business: { id: 9, businessName: "北洋食堂" },
      orderTotal: 40,
      deliveryAddress: address,
      orderState: 1,
      usedVoucher: undefined,
      voucherDiscount: 0,
      pointsUsed: 0,
      pointsDiscount: 0,
      walletPaid: 40,
      paymentMethod: "wallet",
    });
    expect(testContext.successSpy).toHaveBeenCalledWith("下单成功！");
  });

  it("combines voucher, points, and mixed payment in order payload", async () => {
    const address = {
      id: 101,
      contactName: "张三",
      contactTel: "18800000000",
      contactSex: 1,
      address: "天津大学北洋园校区",
    };

    testContext.walletStore.walletBalance = 80;
    testContext.cartStore.itemsForCurrentBusiness = [
      {
        id: 11,
        quantity: 2,
        customer: testContext.authUser,
        business: { id: 9, businessName: "北洋食堂" },
        food: { id: 5, foodName: "宫保鸡丁", foodPrice: 18 },
      },
    ];
    testContext.cartStore.cartTotal = 36;
    testContext.cartStore.deliveryPrice = 4;
    testContext.cartStore.finalOrderTotal = 40;
    testContext.getCurrentUserAddressesSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [address],
    });
    testContext.getMyVouchersSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: [
        {
          id: 201,
          walletId: 1,
          publicVoucherId: 9,
          value: 5,
          threshold: 30,
          expiryDate: "2099-12-31T00:00:00.000Z",
          used: false,
          publicVoucher: {
            id: 9,
            threshold: 30,
            value: 5,
            claimable: true,
            validDays: 30,
          },
        },
      ],
    });
    testContext.addOrderSpy.mockResolvedValue({
      success: true,
      code: "OK",
      data: { id: 701 },
    });

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "selectedAddressId", 101);
    setSetupStateValue(wrapper, "selectedVoucherId", 201);
    setSetupStateValue(wrapper, "usePoints", true);
    setSetupStateValue(wrapper, "pointsToUse", 300);
    setSetupStateValue(wrapper, "paymentMethod", "mixed");
    setSetupStateValue(wrapper, "walletPayAmount", 20);
    setSetupStateValue(wrapper, "activeStep", 3);
    await flushAsync();

    const submitButton = wrapper
      .findAll("button")
      .find((button) => button.text().includes("提交订单"));

    if (!submitButton) {
      throw new Error("Submit order button not found");
    }

    await submitButton.trigger("click");
    await flushAsync();

    expect(testContext.addOrderSpy).toHaveBeenCalledWith({
      customer: testContext.authUser,
      business: { id: 9, businessName: "北洋食堂" },
      orderTotal: 32,
      deliveryAddress: address,
      orderState: 1,
      usedVoucher: { id: 201 },
      voucherDiscount: 5,
      pointsUsed: 300,
      pointsDiscount: 3,
      walletPaid: 20,
      paymentMethod: "mixed",
    });
    expect(testContext.successSpy).toHaveBeenCalledWith("下单成功！");
  });

  it("saves a new address and refreshes the address list", async () => {
    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "isAddressDialogVisible", true);
    setSetupStateValue(wrapper, "addressForm", {
      contactName: "李四",
      contactTel: "17700000000",
      contactSex: 1,
      address: "天津大学卫津路校区",
    });

    const setupState = getSetupState(wrapper);
    await (setupState.saveAddress as () => Promise<void>)();
    await flushAsync();

    expect(testContext.addDeliveryAddressSpy).toHaveBeenCalledWith({
      contactName: "李四",
      contactTel: "17700000000",
      contactSex: 1,
      address: "天津大学卫津路校区",
      customer: testContext.authUser,
    });
    expect(testContext.successSpy).toHaveBeenCalledWith("地址添加成功！");
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(2);
    expect(getSetupStateBoolean(wrapper, "isAddressDialogVisible")).toBe(false);
  });

  it("shows an error and keeps the dialog open when saving address fails", async () => {
    testContext.addDeliveryAddressSpy.mockRejectedValue(new Error("network down"));

    const wrapper = await mountCheckout();

    setSetupStateValue(wrapper, "isAddressDialogVisible", true);
    setSetupStateValue(wrapper, "addressForm", {
      contactName: "李四",
      contactTel: "17700000000",
      contactSex: 1,
      address: "天津大学卫津路校区",
    });

    const setupState = getSetupState(wrapper);
    await (setupState.saveAddress as () => Promise<void>)();
    await flushAsync();

    expect(testContext.addDeliveryAddressSpy).toHaveBeenCalledTimes(1);
    expect(testContext.errorSpy).toHaveBeenCalledWith("保存地址失败: network down");
    expect(testContext.successSpy).not.toHaveBeenCalledWith("地址添加成功！");
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(1);
    expect(getSetupStateBoolean(wrapper, "isAddressDialogVisible")).toBe(true);
  });

  it("deletes an address after confirmation and refreshes the address list", async () => {
    const wrapper = await mountCheckout();
    const setupState = getSetupState(wrapper);

    await (setupState.deleteAddress as (id: number) => Promise<void>)(101);
    await flushAsync();
    await flushAsync();

    expect(testContext.confirmSpy).toHaveBeenCalled();
    expect(testContext.deleteDeliveryAddressSpy).toHaveBeenCalledWith(101);
    expect(testContext.successSpy).toHaveBeenCalledWith("地址删除成功！");
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(2);
  });

  it("shows an error and does not refresh addresses when deleting fails", async () => {
    testContext.deleteDeliveryAddressSpy.mockRejectedValue(new Error("permission denied"));

    const wrapper = await mountCheckout();
    const setupState = getSetupState(wrapper);

    await (setupState.deleteAddress as (id: number) => Promise<void>)(101);
    await flushAsync();
    await flushAsync();

    expect(testContext.confirmSpy).toHaveBeenCalled();
    expect(testContext.deleteDeliveryAddressSpy).toHaveBeenCalledWith(101);
    expect(testContext.errorSpy).toHaveBeenCalledWith("删除地址失败: permission denied");
    expect(testContext.successSpy).not.toHaveBeenCalledWith("地址删除成功！");
    expect(testContext.getCurrentUserAddressesSpy).toHaveBeenCalledTimes(1);
  });
});