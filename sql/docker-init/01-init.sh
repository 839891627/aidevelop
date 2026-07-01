#!/bin/bash
set -e

echo "=== 开始初始化数据库 ==="

mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<-EOSQL
    GRANT ALL PRIVILEGES ON ai_develop.* TO '$MYSQL_USER'@'%';
    FLUSH PRIVILEGES;
EOSQL

for sql_file in \
    /sql-source/demo_tables.sql \
    /sql-source/chat_memory.sql \
    /sql-source/ai_cost_tracking.sql \
    /sql-source/prompt_registry.sql
do
    echo "执行: $(basename $sql_file)"
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "$sql_file"
done

echo "=== 数据库初始化完成 ==="
