import { chromium } from "playwright";
import fs from "fs";
import path from "path";

const outDir = process.env.OUT_DIR;
const member = process.env.MEMBER_TOKEN;
const admin = process.env.ADMIN_TOKEN;
const base = "http://127.0.0.1:5173";
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 1,
});
const page = await context.newPage();
page.setDefaultTimeout(8000);

async function shot(name, url, prepare) {
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 15000 });
  if (prepare) await prepare(page);
  await page.waitForTimeout(400);
  const file = path.join(outDir, name);
  await page.screenshot({ path: file, fullPage: false });
  console.log("OK", name, fs.statSync(file).size);
}

await page.goto(base + "/", { waitUntil: "domcontentloaded" });
await page.evaluate(([m, a]) => {
  localStorage.setItem("ql_token", m);
  localStorage.setItem("ql_admin_token", a);
  localStorage.removeItem("ql_support_cid");
}, [member, admin]);

await shot("01-首页.png", base + "/", async (p) => {
  const first = p.getByRole("button", { name: "第 1 张" });
  if (await first.count()) await first.click().catch(() => {});
});

await shot("02-商品详情.png", base + "/product/1");
await shot("03-分类.png", base + "/category");
await shot("04-我的订单.png", base + "/orders");

await shot("05-客服.png", base + "/support", async (p) => {
  const neu = p.getByRole("button", { name: "新开会话" });
  if (await neu.count()) await neu.click().catch(() => {});
});

await shot("06-客服-政策问答.png", base + "/support", async (p) => {
  await p.getByRole("button", { name: "问退货政策" }).click();
  await p.getByText("知识来源").first().waitFor({ timeout: 12000 }).catch(() => {});
});

await shot("07-后台-今日店况.png", base + "/admin");
await shot("08-后台-商品.png", base + "/admin/products");
await shot("09-后台-订单.png", base + "/admin/orders");

await browser.close();
console.log("DONE");
