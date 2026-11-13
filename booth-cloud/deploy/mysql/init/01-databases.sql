-- 仅在 MySQL 数据卷首次初始化时执行；表结构由各服务启动时通过 Flyway 创建
CREATE DATABASE IF NOT EXISTS booth_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS booth_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS booth_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 应用账号只拥有自己业务库的权限
CREATE USER IF NOT EXISTS 'booth'@'%' IDENTIFIED BY 'booth123';
GRANT ALL PRIVILEGES ON booth_user.* TO 'booth'@'%';
GRANT ALL PRIVILEGES ON booth_product.* TO 'booth'@'%';
GRANT ALL PRIVILEGES ON booth_trade.* TO 'booth'@'%';

-- Canal 伪装为从库订阅 binlog，只需复制相关权限
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED WITH mysql_native_password BY 'canal123';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';

FLUSH PRIVILEGES;
