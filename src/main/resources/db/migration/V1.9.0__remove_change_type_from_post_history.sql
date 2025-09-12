-- Remove change_type column from post_histories table
ALTER TABLE post_histories
DROP COLUMN change_type;