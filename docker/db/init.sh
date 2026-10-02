#!/bin/bash
# SQL Server の起動を待ってから init.sql を実行する（2019 イメージの sqlcmd パスに対応）
set -e
SQLCMD=/opt/mssql-tools/bin/sqlcmd
if [ ! -x "$SQLCMD" ]; then SQLCMD=/opt/mssql-tools18/bin/sqlcmd; fi
OPTS=""
if [[ "$SQLCMD" == *tools18* ]]; then OPTS="-C"; fi
for i in $(seq 1 60); do
  if $SQLCMD $OPTS -S db -U sa -P "$MSSQL_SA_PASSWORD" -Q "SELECT 1" > /dev/null 2>&1; then
    echo "SQL Server に接続できました。初期化を実行します。"
    $SQLCMD $OPTS -S db -U sa -P "$MSSQL_SA_PASSWORD" -v APP_DB_PASSWORD="$APP_DB_PASSWORD" -i /docker/init.sql
    echo "初期化が完了しました。"
    exit 0
  fi
  echo "SQL Server の起動を待っています... ($i)"
  sleep 3
done
echo "SQL Server に接続できませんでした。" >&2
exit 1
