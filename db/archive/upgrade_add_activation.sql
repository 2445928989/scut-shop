-- Add activation token fields for email activation feature
ALTER TABLE `user` 
  ADD COLUMN IF NOT EXISTS `activation_token` VARCHAR(128) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `activation_expires` DATETIME DEFAULT NULL;

-- Note: Run this once against your database if upgrading an existing installation.
