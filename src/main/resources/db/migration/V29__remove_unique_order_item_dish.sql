---cho phép nhiều OrderItem cùng dishId trong một order.
ALTER TABLE order_item
DROP CONSTRAINT IF EXISTS uq_order_item_order_dish;
