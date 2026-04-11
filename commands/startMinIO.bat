@echo off
REM 移动到MinIO安装目录
cd /d C:
cd "C:\MinIO"
REM 启动redis
start cmd /k minio.exe server "C:\MinIO\file"
exit