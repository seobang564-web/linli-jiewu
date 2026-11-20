#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo '请在服务器终端使用 sudo bash deploy.sh 运行。' >&2
    exit 1
fi
if [[ $# -gt 1 || ($# -eq 1 && "${1:-}" != '--configure') ]]; then
    echo '用法：sudo bash deploy.sh [--configure]' >&2
    exit 1
fi

package_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd -- "$package_dir"
sha256sum --check SHA256SUMS

for tool in python3 openssl curl systemctl mysql; do
    if ! command -v "$tool" >/dev/null; then
        echo "缺少 $tool；请先安装对应 Ubuntu 软件包。" >&2
        exit 1
    fi
done
java_bin="/usr/lib/jvm/java-17-openjdk-$(dpkg --print-architecture)/bin/java"
if [[ ! -x "$java_bin" ]] || ! "$java_bin" -version 2>&1 | grep -q 'version "17\.'; then
    echo '请先执行 sudo apt update && sudo apt install -y openjdk-17-jre-headless。' >&2
    exit 1
fi
if ! systemctl is-active --quiet mysql; then
    echo 'MySQL 尚未运行，请先检查 sudo systemctl status mysql。' >&2
    exit 1
fi

# Validate the additive migration before replacing the running application's JAR.
if ! mysql --batch --skip-column-names -e \
    'SELECT image_id, owner_id, file_name, size_bytes, created_at FROM linli_jiewu.images LIMIT 0' >/dev/null; then
    echo '图片表未就绪或当前 root 无法通过 MySQL socket 核验。请先执行 sudo mysql < 002_images.sql，并确认数据库访问权限。现有 JAR 未替换。' >&2
    exit 1
fi

if ! id linli >/dev/null 2>&1; then
    adduser --system --group --no-create-home linli
fi
install -d -o root -g root -m 755 /opt/linli
install -d -o linli -g linli -m 750 /opt/linli/uploads
install -d -o root -g root -m 700 /etc/linli

if [[ ! -f /etc/linli/server.env || "${1:-}" == '--configure' ]]; then
    IFS= read -r -s -p '请输入 MySQL 用户 linli 的密码（输入不显示）：' database_password </dev/tty
    printf '\n' >/dev/tty
    if [[ -z "$database_password" ]]; then
        echo '数据库密码不能为空。' >&2
        exit 1
    fi
    jwt_secret=$(openssl rand -base64 48 | tr -d '\n')
    if [[ -f /etc/linli/server.env ]]; then
        cp --preserve=mode,ownership /etc/linli/server.env /etc/linli/server.env.previous
    fi
    DB_PASSWORD="$database_password" JWT_SECRET="$jwt_secret" python3 - <<'PY'
import os
from pathlib import Path

def quoted(value):
    return '"' + value.replace('\\', '\\\\').replace('"', '\\"') + '"'

values = {
    'DB_USERNAME': 'linli',
    'DB_PASSWORD': os.environ['DB_PASSWORD'],
    'JWT_SECRET': os.environ['JWT_SECRET'],
    'UPLOAD_DIR': '/opt/linli/uploads',
}
path = Path('/etc/linli/server.env')
descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(descriptor, 'w', encoding='utf-8') as output:
    for key, value in values.items():
        output.write(f'{key}={quoted(value)}\n')
os.chmod(path, 0o600)
PY
    unset database_password jwt_secret
else
    echo '沿用已有环境配置；如需修改数据库密码，使用 --configure 参数重新运行。'
fi

if [[ -f /opt/linli/app.jar ]]; then
    cp --preserve=mode,ownership /opt/linli/app.jar /opt/linli/app.jar.previous
fi
install -o root -g linli -m 640 app.jar /opt/linli/app.jar
if [[ ! -f /opt/linli/application-prod.yml ]]; then
    install -o root -g linli -m 640 application-prod.yml /opt/linli/application-prod.yml
fi
chown root:linli /opt/linli/application-prod.yml
chmod 640 /opt/linli/application-prod.yml
sed "s|/usr/bin/java|$java_bin|g" linli.service > /etc/systemd/system/linli.service
chmod 644 /etc/systemd/system/linli.service
systemctl daemon-reload
systemctl enable linli
systemctl restart linli

hello_ready=false
for attempt in $(seq 1 30); do
    if curl --fail --silent --max-time 2 http://127.0.0.1:8080/api/hello >/dev/null; then
        hello_ready=true
        break
    fi
    sleep 1
done
if [[ "$hello_ready" != true ]]; then
    echo '服务尚未正常响应，请执行 sudo journalctl -u linli -n 80 --no-pager。' >&2
    exit 1
fi
echo 'Hello 接口：'
curl --fail --silent --show-error --max-time 8 http://127.0.0.1:8080/api/hello
printf '\n数据库健康检查：\n'
if ! curl --fail-with-body --silent --show-error --max-time 8 http://127.0.0.1:8080/api/health; then
    printf '\n数据库检查失败：确认 linli 的密码、权限和七张表（含 images）。可用 --configure 重填密码；无需重新建表。\n' >&2
    exit 1
fi
printf '\n部署成功。还需从本机验证公网访问，并在腾讯云防火墙放行 TCP 8080。\n'
