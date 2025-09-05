-- Ocean Shopping Center - Database Index Optimization
-- Performance-critical indexes for high-concurrency scenarios

-- ====================
-- USER MANAGEMENT INDEXES
-- ====================

-- User authentication and profile lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_active 
ON users(email) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_active 
ON users(username) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_date 
ON users(created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_last_login 
ON users(last_login DESC) WHERE last_login IS NOT NULL;

-- User roles for authorization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_roles_user_id 
ON user_roles(user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_roles_role_name 
ON user_roles(role_name);

-- ====================
-- PRODUCT CATALOG INDEXES
-- ====================

-- Product search and filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_name_trgm 
ON products USING gin(name gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_description_trgm 
ON products USING gin(description gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_category_active 
ON products(category_id, is_active) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_price_range 
ON products(price) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_created_date 
ON products(created_date DESC) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_rating_count 
ON products(average_rating DESC, review_count DESC) WHERE is_active = true;

-- Product inventory tracking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_stock_status 
ON products(stock_quantity, is_active) WHERE is_active = true AND stock_quantity >= 0;

-- Product categories
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_categories_parent_active 
ON categories(parent_id, is_active) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_categories_name_active 
ON categories(name) WHERE is_active = true;

-- ====================
-- SHOPPING CART INDEXES
-- ====================

-- Cart operations by user and session
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cart_items_user_id 
ON cart_items(user_id) WHERE user_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cart_items_session_id 
ON cart_items(session_id) WHERE session_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cart_items_product_user 
ON cart_items(product_id, user_id) WHERE user_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cart_items_updated_date 
ON cart_items(updated_date DESC);

-- ====================
-- ORDER PROCESSING INDEXES
-- ====================

-- Order lookups by user and status
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_date 
ON orders(user_id, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_status_date 
ON orders(status, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_order_number 
ON orders(order_number);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_payment_status 
ON orders(payment_status, created_date DESC);

-- Order items for order details
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_items_order_id 
ON order_items(order_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_items_product_id 
ON order_items(product_id);

-- ====================
-- PAYMENT PROCESSING INDEXES
-- ====================

-- Payment transactions
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_order_id 
ON payments(order_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_status_date 
ON payments(status, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_transaction_id 
ON payments(transaction_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_payment_method 
ON payments(payment_method, created_date DESC);

-- Payment gateway logs for debugging
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payment_logs_payment_id 
ON payment_logs(payment_id, created_date DESC);

-- ====================
-- SHIPPING & LOGISTICS INDEXES
-- ====================

-- Shipping information
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shipping_info_order_id 
ON shipping_info(order_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shipping_info_tracking 
ON shipping_info(tracking_number) WHERE tracking_number IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shipping_info_status_date 
ON shipping_info(status, updated_date DESC);

-- Shipping rates cache
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shipping_rates_cache_key 
ON shipping_rates_cache(cache_key, expires_at);

-- ====================
-- REVIEW SYSTEM INDEXES
-- ====================

-- Product reviews
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reviews_product_approved 
ON reviews(product_id, is_approved, created_date DESC) WHERE is_approved = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reviews_user_date 
ON reviews(user_id, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reviews_rating_date 
ON reviews(rating DESC, created_date DESC) WHERE is_approved = true;

-- ====================
-- COUPON SYSTEM INDEXES
-- ====================

-- Coupon usage and validation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_code_active 
ON coupons(code) WHERE is_active = true AND expires_at > NOW();

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_type_active 
ON coupons(coupon_type, is_active) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupon_usage_user_coupon 
ON coupon_usage(user_id, coupon_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupon_usage_order_id 
ON coupon_usage(order_id);

-- ====================
-- NOTIFICATION SYSTEM INDEXES
-- ====================

-- User notifications
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_user_unread 
ON notifications(user_id, is_read, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_type_date 
ON notifications(notification_type, created_date DESC);

-- ====================
-- CHAT SYSTEM INDEXES
-- ====================

-- Chat messages
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_user_date 
ON chat_messages(user_id, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_room_date 
ON chat_messages(chat_room_id, created_date DESC);

-- Chat rooms
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_rooms_user_active 
ON chat_rooms(user_id, is_active) WHERE is_active = true;

-- ====================
-- SESSION MANAGEMENT INDEXES
-- ====================

-- User sessions for Redis fallback
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_user_active 
ON user_sessions(user_id, is_active) WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_token_hash 
ON user_sessions(session_token_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_expires_at 
ON user_sessions(expires_at DESC);

-- ====================
-- AUDIT LOGGING INDEXES
-- ====================

-- Audit logs for compliance and debugging
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_user_action 
ON audit_logs(user_id, action, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_entity_type 
ON audit_logs(entity_type, entity_id, created_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_date 
ON audit_logs(created_date DESC);

-- ====================
-- PERFORMANCE STATISTICS
-- ====================

-- Collect statistics on new indexes
ANALYZE users, products, cart_items, orders, order_items, payments, reviews, coupons;

-- Create partial indexes for frequently queried subsets
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_recent_orders 
ON orders(created_date DESC) WHERE created_date > (CURRENT_DATE - INTERVAL '30 days');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_high_value_orders 
ON orders(total_amount DESC) WHERE total_amount > 100.00 AND status = 'COMPLETED';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_popular_products 
ON products(view_count DESC, purchase_count DESC) 
WHERE is_active = true AND view_count > 100;

-- ====================
-- INDEX MAINTENANCE QUERIES
-- ====================

-- Check index usage (run periodically to identify unused indexes)
-- SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch 
-- FROM pg_stat_user_indexes 
-- WHERE idx_tup_read = 0 AND idx_tup_fetch = 0
-- ORDER BY schemaname, tablename;

-- Check index size
-- SELECT schemaname, tablename, indexname, 
--        pg_size_pretty(pg_relation_size(indexname::regclass)) as size
-- FROM pg_stat_user_indexes 
-- ORDER BY pg_relation_size(indexname::regclass) DESC;

-- Monitor index bloat (requires pgstattuple extension)
-- SELECT schemaname, tablename, indexname,
--        round((case when avg_leaf_density = 0 then 0 
--               else 100 * (100 - avg_leaf_density) / 100 end), 2) as bloat_ratio
-- FROM pgstatindex(indexname);