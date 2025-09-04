---
issue: 7
stream: Coupon System & Additional Features
agent: general-purpose
started: 2025-09-04T19:10:00Z
status: in_progress
---

# Stream 4: Coupon System & Additional Features

## Scope
Discount system, invoice generation, refund handling, and additional order management features.

## Files
- backend/src/main/java/com/ocean/shopping/model/entity/Coupon.java
- backend/src/main/java/com/ocean/shopping/model/entity/OrderCoupon.java
- backend/src/main/java/com/ocean/shopping/repository/CouponRepository.java
- backend/src/main/java/com/ocean/shopping/repository/OrderCouponRepository.java
- backend/src/main/java/com/ocean/shopping/service/CouponService.java
- backend/src/main/java/com/ocean/shopping/service/InvoiceService.java
- backend/src/main/java/com/ocean/shopping/service/RefundService.java
- backend/src/main/java/com/ocean/shopping/controller/CouponController.java
- frontend/src/components/coupons/CouponInput.tsx
- frontend/src/components/coupons/DiscountSummary.tsx
- frontend/src/services/couponService.ts

## Progress
- Starting implementation of coupon system and additional features
- Will integrate with completed payment system (Stream 2) and order system (Stream 3)