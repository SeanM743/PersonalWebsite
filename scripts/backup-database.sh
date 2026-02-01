#!/bin/bash
# Database Backup Script
# Creates a timestamped backup of the PostgreSQL database before any destructive operations

set -e

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups"
BACKUP_FILE="${BACKUP_DIR}/personal_platform_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p ${BACKUP_DIR}

echo "🔄 Starting database backup..."
echo "📦 Backup file: ${BACKUP_FILE}"

# Create backup using pg_dump from within the container
docker exec personal_db pg_dump -U admin personal_platform > ${BACKUP_FILE}

# Check if backup was successful
if [ -f "${BACKUP_FILE}" ]; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "✅ Database backed up successfully!"
    echo "📊 Backup size: ${BACKUP_SIZE}"
    echo "📁 Location: ${BACKUP_FILE}"
else
    echo "❌ Backup failed!"
    exit 1
fi
