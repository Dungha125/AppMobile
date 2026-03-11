-- Migration: Thêm các fields mới cho system_config table
-- Run this if your system_config table already exists without these fields

USE eldercare;

-- Kiểm tra và thêm column display_name
SET @query = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'eldercare' 
     AND TABLE_NAME = 'system_config' 
     AND COLUMN_NAME = 'display_name') = 0,
    'ALTER TABLE system_config ADD COLUMN display_name VARCHAR(255) AFTER config_value',
    'SELECT "Column display_name already exists" AS message'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra và thêm column category
SET @query = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'eldercare' 
     AND TABLE_NAME = 'system_config' 
     AND COLUMN_NAME = 'category') = 0,
    'ALTER TABLE system_config ADD COLUMN category VARCHAR(50) AFTER display_name',
    'SELECT "Column category already exists" AS message'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra và thêm column config_type
SET @query = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'eldercare' 
     AND TABLE_NAME = 'system_config' 
     AND COLUMN_NAME = 'config_type') = 0,
    'ALTER TABLE system_config ADD COLUMN config_type VARCHAR(20) DEFAULT ''string'' AFTER description',
    'SELECT "Column config_type already exists" AS message'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Cập nhật description column để có thể chứa text dài hơn
ALTER TABLE system_config MODIFY COLUMN description TEXT;

-- Verify changes
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'eldercare' 
AND TABLE_NAME = 'system_config'
ORDER BY ORDINAL_POSITION;

SELECT '✅ Migration completed successfully!' AS status;
