#!/usr/bin/env bash
# 生成 deploy/.env（含 Nacos 必填 token）。已存在则拒绝覆盖，除非传入 --force。
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
EXAMPLE="$HERE/.env.example"
TARGET="$HERE/.env"
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

if [[ ! -f "$EXAMPLE" ]]; then
  echo "缺少 $EXAMPLE" >&2
  exit 1
fi
if [[ -f "$TARGET" && "$FORCE" -ne 1 ]]; then
  echo "已存在 deploy/.env。若要重生成请加 --force（会覆盖）。"
  exit 0
fi

token="$(openssl rand -base64 32 | tr -d '\n')"
identity="$(openssl rand -hex 16)"

awk -v token="$token" -v identity="$identity" '
  /^[[:space:]]*NACOS_AUTH_TOKEN[[:space:]]*=/ { print "NACOS_AUTH_TOKEN=" token; next }
  /^[[:space:]]*NACOS_AUTH_IDENTITY_VALUE[[:space:]]*=/ { print "NACOS_AUTH_IDENTITY_VALUE=" identity; next }
  /^[[:space:]]*BOOTH_PAY_MOCK_ENABLED[[:space:]]*=/ { print "BOOTH_PAY_MOCK_ENABLED=true"; next }
  /^[[:space:]]*BOOTH_PAY_ALLOW_DEV_SECRET[[:space:]]*=/ { print "BOOTH_PAY_ALLOW_DEV_SECRET=true"; next }
  /^[[:space:]]*API_DOCS_ENABLED[[:space:]]*=/ { print "API_DOCS_ENABLED=true"; next }
  /^[[:space:]]*MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE[[:space:]]*=/ { print "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info"; next }
  { print }
' "$EXAMPLE" > "$TARGET"

echo "已写入 $TARGET"
echo "NACOS_AUTH_TOKEN 与 NACOS_AUTH_IDENTITY_VALUE 已随机生成；勿提交到 Git。"
echo "本机演示已默认打开：Mock 支付 + API 文档。Actuator 仍仅 health,info。"
