-- 開発用 DB の初期化（docker compose の db-init サービスが sqlcmd で実行する）
-- データベース appmgmt とログイン appmgmt を作る。テーブルは Web アプリ起動時に Flyway が作成する。
IF DB_ID('appmgmt') IS NULL
BEGIN
    CREATE DATABASE appmgmt COLLATE Japanese_CI_AS;
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'appmgmt')
BEGIN
    CREATE LOGIN appmgmt WITH PASSWORD = '$(APP_DB_PASSWORD)', CHECK_POLICY = OFF, DEFAULT_DATABASE = appmgmt;
END
GO
USE appmgmt;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'appmgmt')
BEGIN
    CREATE USER appmgmt FOR LOGIN appmgmt;
    ALTER ROLE db_owner ADD MEMBER appmgmt;
END
GO
