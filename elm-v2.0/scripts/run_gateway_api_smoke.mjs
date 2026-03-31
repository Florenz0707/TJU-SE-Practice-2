#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

function parseArgs(argv) {
  const args = { envFile: null };
  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index];
    if (current === "--env-file") {
      args.envFile = argv[index + 1] ?? null;
      index += 1;
    }
  }
  return args;
}

function loadEnvFile(envFile) {
  if (!envFile) return;
  const content = fs.readFileSync(envFile, "utf8");
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eqIndex = line.indexOf("=");
    if (eqIndex === -1) continue;
    const key = line.slice(0, eqIndex).trim();
    const value = line.slice(eqIndex + 1).trim();
    if (!(key in process.env)) {
      process.env[key] = value;
    }
  }
}

function env(name, fallback = undefined) {
  const value = process.env[name] ?? fallback;
  if (value === undefined || value === "") {
    throw new Error(`Missing required env: ${name}`);
  }
  return value;
}

async function requestJson(baseUrl, apiPath, { method = "GET", token, body, headers = {} } = {}) {
  const response = await fetch(new URL(apiPath, baseUrl), {
    method,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let json;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    throw new Error(`${method} ${apiPath} returned non-JSON (${response.status}): ${text}`);
  }
  if (!response.ok) {
    throw new Error(`${method} ${apiPath} failed ${response.status}: ${JSON.stringify(json)}`);
  }
  return json;
}

function assertSuccess(name, response) {
  if (!response || response.success !== true) {
    throw new Error(`${name} failed: ${JSON.stringify(response)}`);
  }
  return response.data;
}

async function login(baseUrl, username, password) {
  const response = await requestJson(baseUrl, "/api/auth", {
    method: "POST",
    body: { username, password, rememberMe: true },
  });
  if (!response?.id_token) {
    throw new Error(`Login failed for ${username}: ${JSON.stringify(response)}`);
  }
  return response.id_token;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const defaultEnvFile = path.resolve(path.dirname(new URL(import.meta.url).pathname), "gateway-api-smoke.env");
  loadEnvFile(args.envFile ? path.resolve(process.cwd(), args.envFile) : (fs.existsSync(defaultEnvFile) ? defaultEnvFile : null));

  const baseUrl = env("GATEWAY_URL", "http://localhost:8090").replace(/\/$/, "");
  const username = `${env("SMOKE_USERNAME_PREFIX", "smoke")}_${Date.now()}`;
  const password = env("SMOKE_PASSWORD", "password");
  const topupAmount = Number(env("SMOKE_TOPUP_AMOUNT", "100"));
  const orderAmount = env("SMOKE_ORDER_AMOUNT", "28.00");
  const businessId = Number(env("SMOKE_BUSINESS_ID", "1"));
  const foodId = Number(env("SMOKE_FOOD_ID", "1"));
  const adminUsername = env("SMOKE_ADMIN_USERNAME", "admin");
  const adminPassword = env("SMOKE_ADMIN_PASSWORD", "123456");

  const summary = { username };

  const user = assertSuccess(
    "createUser",
    await requestJson(baseUrl, "/api/persons", { method: "POST", body: { username } }),
  );
  summary.userId = user.id;

  const userToken = await login(baseUrl, username, password);
  summary.userLogin = true;

  const walletBefore = assertSuccess("walletBefore", await requestJson(baseUrl, "/api/wallet/my", { token: userToken }));
  summary.walletId = walletBefore.id;
  summary.walletBefore = walletBefore.balance;

  const topup = assertSuccess(
    "topup",
    await requestJson(baseUrl, "/api/wallet/my/topup", { method: "POST", token: userToken, body: topupAmount }),
  );
  summary.topupTransactionId = topup.id;

  const walletAfterTopup = assertSuccess("walletAfterTopup", await requestJson(baseUrl, "/api/wallet/my", { token: userToken }));
  summary.walletAfterTopup = walletAfterTopup.balance;

  const address = assertSuccess(
    "createAddress",
    await requestJson(baseUrl, "/api/addresses", {
      method: "POST",
      token: userToken,
      body: {
        contactName: env("SMOKE_CONTACT_NAME", "Smoke User"),
        contactSex: Number(env("SMOKE_CONTACT_SEX", "1")),
        contactTel: env("SMOKE_CONTACT_TEL", "18800000000"),
        address: env("SMOKE_ADDRESS", "TJU Smoke Address"),
      },
    }),
  );
  summary.addressId = address.id;

  const cart1 = assertSuccess(
    "cart1",
    await requestJson(baseUrl, "/api/carts", {
      method: "POST",
      token: userToken,
      body: { food: { id: foodId }, quantity: 1 },
    }),
  );
  summary.cart1Id = cart1.id;

  const order1 = assertSuccess(
    "order1",
    await requestJson(baseUrl, "/api/orders", {
      method: "POST",
      token: userToken,
      headers: { "X-Request-Id": `gateway-smoke-order-1-${Date.now()}` },
      body: { business: { id: businessId }, deliveryAddress: { id: address.id }, walletPaid: orderAmount },
    }),
  );
  summary.order1Id = order1.id;

  const cancelOrder = assertSuccess(
    "cancelOrder1",
    await requestJson(baseUrl, `/api/orders/${order1.id}/cancel`, { method: "POST", token: userToken }),
  );
  summary.cancel1State = cancelOrder.orderState;

  const cart2 = assertSuccess(
    "cart2",
    await requestJson(baseUrl, "/api/carts", {
      method: "POST",
      token: userToken,
      body: { food: { id: foodId }, quantity: 1 },
    }),
  );
  summary.cart2Id = cart2.id;

  const order2 = assertSuccess(
    "order2",
    await requestJson(baseUrl, "/api/orders", {
      method: "POST",
      token: userToken,
      headers: { "X-Request-Id": `gateway-smoke-order-2-${Date.now()}` },
      body: { business: { id: businessId }, deliveryAddress: { id: address.id }, walletPaid: orderAmount },
    }),
  );
  summary.order2Id = order2.id;

  let completeOrder;
  let completeActor = "user";
  try {
    completeOrder = assertSuccess(
      "completeOrder2User",
      await requestJson(baseUrl, "/api/orders", {
        method: "PATCH",
        token: userToken,
        body: { id: order2.id, orderState: 4 },
      }),
    );
  } catch {
    const adminToken = await login(baseUrl, adminUsername, adminPassword);
    completeActor = adminUsername;
    completeOrder = assertSuccess(
      "completeOrder2Admin",
      await requestJson(baseUrl, "/api/orders", {
        method: "PATCH",
        token: adminToken,
        body: { id: order2.id, orderState: 4 },
      }),
    );
  }
  summary.completeActor = completeActor;
  summary.complete2State = completeOrder.orderState;

  const review = assertSuccess(
    "review",
    await requestJson(baseUrl, `/api/reviews/order/${order2.id}`, {
      method: "POST",
      token: userToken,
      body: {
        stars: Number(env("SMOKE_REVIEW_STARS", "5")),
        content: env("SMOKE_REVIEW_CONTENT", "smoke review"),
        anonymous: false,
      },
    }),
  );
  summary.reviewId = review.id;

  const reviewFetched = assertSuccess(
    "reviewFetched",
    await requestJson(baseUrl, `/api/reviews/order/${order2.id}`, { token: userToken }),
  );
  summary.reviewFetchedId = reviewFetched.id;

  assertSuccess(
    "deleteReview",
    await requestJson(baseUrl, `/api/reviews/${review.id}`, { method: "DELETE", token: userToken }),
  );
  summary.reviewDeleted = true;

  const orders = assertSuccess("orders", await requestJson(baseUrl, "/api/orders/user/my", { token: userToken }));
  summary.orderCount = Array.isArray(orders) ? orders.length : -1;

  const walletFinal = assertSuccess("walletFinal", await requestJson(baseUrl, "/api/wallet/my", { token: userToken }));
  summary.walletFinal = walletFinal.balance;

  console.log(JSON.stringify(summary, null, 2));
}

main().catch((error) => {
  console.error(`SMOKE_OK=false\nERROR=${error instanceof Error ? error.message : String(error)}`);
  process.exit(2);
});