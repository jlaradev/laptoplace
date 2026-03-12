# LaptopHub

Full-stack laptop e-commerce built as a portfolio project. Covers the complete purchase flow — catalog browsing, cart management, Stripe checkout, order tracking, and a fully functional admin panel — deployed and production-ready.

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-DD0031?style=flat-square&logo=angular&logoColor=white)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Stripe](https://img.shields.io/badge/Stripe-test_mode-635BFF?style=flat-square&logo=stripe&logoColor=white)](https://stripe.com/)

> **Note on naming:** The repository was originally called `laptophub`. After discovering that name was already taken, the repo was renamed to `laptoplace`, but the backend domain was kept as-is. The backend is still this project.

| | |
|---|---|
| **Frontend** | https://laptoplace.vercel.app/ |
| **Backend** | https://laptophub-cigv.onrender.com/ |

Anyone can create an account and go through the full purchase flow. The admin panel is deployed but credentials are not published — available on request for evaluation purposes.

---

## Tech Stack

| Technology | Role | Why |
|---|---|---|
| **Spring Boot 3** | Backend framework | Production-grade ecosystem with Spring Security and Spring Data JPA as first-class citizens. Layered architecture (controllers → services → repositories) maps naturally to the framework's conventions, avoiding manual wiring. |
| **Angular 19** | Frontend framework | Opinionated structure that enforces clean separation between components, services, and routing. Lazy loading and route guards were applied across the admin module to keep the bundle lean and access properly gated. |
| **PostgreSQL** | Database | Relational model to handle the associations between users, orders, products, and payments with proper foreign key constraints and transactional integrity. |
| **JWT (JJWT)** | Authentication | Stateless auth — each request carries a signed token, so there is no session state on the server and no shared-session issues when scaling horizontally. |
| **Stripe** | Payments | Integrated via PaymentIntent API with a real webhook listener. The checkout creates a PaymentIntent on the backend, confirms it through Stripe Elements on the frontend, and the webhook updates order/payment status server-side. |
| **Cloudinary** | Image storage | Avoids storing binary data in the database and offloads image optimization and CDN delivery. |
| **Brevo** | Transactional email | REST API integration for password reset emails. |
| **Bucket4j** | Rate limiting | In-memory rate limiter on the login endpoint — 5 failed attempts per IP per 15 minutes — without requiring Redis or any external dependency. |
| **Docker** | Containerization | Backend containerized for consistent deployment regardless of host environment. |

---

## Features

**Storefront**
- Catalog with search by name, filter by brand, and sorting by price, rating, or newest
- Product detail page with image gallery and paginated reviews
- Side-by-side product comparison
- Persistent shopping cart
- Stripe checkout (test mode) with real PaymentIntent flow and webhook handling
- Order history with live status tracking

**Account**
- Registration and login with JWT authentication
- Password reset via tokenized email link (30-minute expiry, single-use, resistant to user enumeration)
- Profile editing and in-app password change
- Review system: only users with a delivered order for that product can leave a review

**Admin panel** (`/admin`, role-gated)
- Dashboard overview
- Full CRUD for products and brands, with Cloudinary image uploads
- User management: create, edit, deactivate/reactivate, assign roles
- Order management: view detail, manually advance to shipped or delivered
- Soft delete across products, brands, and users, with restore capability

**Backend internals**
- Stateless session management (JWT on every request)
- Rate limiting on login via Bucket4j: 5 failed attempts per IP within a 15-minute window. On the 6th failed attempt, the request is rejected with HTTP 429 before authentication is attempted
- Scheduled jobs for order lifecycle simulation (see [Scheduled Jobs](#scheduled-jobs))
- Global exception handler with consistent HTTP error responses
- CSRF disabled, CORS configured, method-level security via `@PreAuthorize`

---

## Project Structure

```
laptoplace/
├── backend/
│   └── src/main/java/com/laptophub/backend/
│       ├── controller/       REST endpoints
│       ├── service/          Business logic
│       ├── model/            JPA entities
│       ├── dto/              Request and response objects
│       ├── repository/       Spring Data interfaces
│       ├── security/         JWT filter, user details, rate limiter
│       ├── scheduler/        Order expiration and progression jobs
│       ├── config/           Stripe configuration
│       └── exception/        Global error handling
└── frontend/
    └── src/app/
        ├── pages/            User-facing views
        ├── admin/            Admin panel (lazy-loaded, route-guarded)
        ├── payment/          Stripe Elements integration
        ├── services/         HTTP services
        ├── models/           TypeScript interfaces
        ├── components/       Shared components (header, footer, product card)
        └── pipes/            Custom pipes
```

---

## Domain Model

| Entity | Description |
|---|---|
| `User` | UUID-identified user with role `USER` or `ADMIN` |
| `Product` | Laptop with name, description, price, stock, brand, and computed rating |
| `Brand` | Laptop manufacturer with name and logo |
| `Cart` / `CartItem` | Per-user persistent cart |
| `Order` / `OrderItem` | Purchase record with status lifecycle and item snapshot |
| `Payment` | Stripe PaymentIntent wrapper with local status tracking |
| `Review` | Rating and text from a verified purchaser |
| `PasswordResetToken` | Single-use UUID token with 30-minute TTL |

**Order status flow**

```
PENDIENTE_PAGO  ──►  PROCESANDO  ──►  ENVIADO  ──►  ENTREGADO
     │
     ▼
CANCELADO / EXPIRADO
```

**Payment status flow**

```
PENDIENTE  ──►  COMPLETADO
     │
     └─────►  FALLIDO
     │
     └─────►  EXPIRADO
```

---

## Frontend Routes

### User routes

| Path | Component | Description |
|---|---|---|
| `/` | `App` | Home page |
| `/login` | `LoginComponent` | User login |
| `/register` | `RegisterComponent` | Registration |
| `/reset-password` | `ResetPasswordComponent` | Password reset via email token |
| `/catalog` | `CatalogComponent` | Product listing with search and filters |
| `/product/:id` | `ProductDetailComponent` | Product detail, gallery, reviews |
| `/product/:productId/review` | `CreateReviewComponent` | Write or edit a review |
| `/my-reviews` | `MyReviewsComponent` | Reviews written by the current user |
| `/compare` | `CompareComponent` | Side-by-side product comparison |
| `/cart` | `CartPageComponent` | Cart |
| `/payment` | `PaymentComponent` | Stripe checkout |
| `/profile` | `ProfileComponent` | Account settings |
| `/orders` | `OrdersComponent` | Order history |

### Admin routes — all protected by `AdminGuard`, require role `ADMIN`

| Path | Component | Description |
|---|---|---|
| `/admin/login` | `AdminLoginComponent` | Admin-specific login |
| `/admin/dashboard` | `AdminDashboardComponent` | Overview panel |
| `/admin/products` | `ProductsListComponent` | Active and inactive products |
| `/admin/products/create` | `ProductFormComponent` | Create product |
| `/admin/products/edit/:id` | `ProductFormComponent` | Edit product |
| `/admin/brands` | `BrandsListComponent` | Brand list |
| `/admin/brands/create` | `BrandFormComponent` | Create brand |
| `/admin/brands/edit/:id` | `BrandFormComponent` | Edit brand |
| `/admin/orders` | `OrdersListComponent` | All orders |
| `/admin/orders/:id` | `OrderDetailComponent` | Order detail and status actions |
| `/admin/users` | `UsersListComponent` | User list |
| `/admin/users/create` | `UserFormComponent` | Create user |
| `/admin/users/:id` | `UserDetailComponent` | User detail and role management |

---

## API Reference

**Base URL:** `https://laptophub-cigv.onrender.com`

**Auth legend:** `Public` — no token required · `Auth` — valid JWT · `Admin` — JWT with role `ADMIN`

<details>
<summary><strong>Authentication</strong> — <code>/api/auth</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Returns JWT + user metadata. Consumes rate limit token on failed credentials |
| `POST` | `/api/auth/forgot-password` | Public | Sends password reset link. Always returns 200 to prevent user enumeration |
| `POST` | `/api/auth/reset-password` | Public | Validates single-use token and updates password |

</details>

<details>
<summary><strong>Users</strong> — <code>/api/users</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/users/register` | Public | Create account |
| `GET` | `/api/users/{id}` | Auth | Get user by UUID |
| `GET` | `/api/users/email?email=` | Auth | Get user by email |
| `GET` | `/api/users` | Admin | Paginated list of active users |
| `GET` | `/api/users/inactive` | Admin | Paginated list of inactive users |
| `PUT` | `/api/users/{id}` | Auth | Update profile data |
| `PATCH` | `/api/users/{id}/password` | Auth | Change password |
| `PATCH` | `/api/users/{id}/role` | Admin | Change user role |
| `DELETE` | `/api/users/{id}` | Admin | Soft-delete user |
| `PUT` | `/api/users/{id}/reactivate` | Admin | Restore user |

</details>

<details>
<summary><strong>Brands</strong> — <code>/api/brands</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/brands` | Public | Paginated active brands |
| `GET` | `/api/brands/{id}` | Public | Brand by ID |
| `GET` | `/api/brands/inactive` | Admin | Paginated inactive brands |
| `POST` | `/api/brands` | Admin | Create brand |
| `PUT` | `/api/brands/{id}` | Admin | Update brand |
| `POST` | `/api/brands/{id}/image` | Admin | Upload brand logo to Cloudinary (multipart) |
| `DELETE` | `/api/brands/{id}` | Admin | Soft-delete brand |
| `PUT` | `/api/brands/{id}/reactivate` | Admin | Restore brand |

</details>

<details>
<summary><strong>Products</strong> — <code>/api/products</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/products` | Public | Unified search with optional filters (see below) |
| `GET` | `/api/products/top-rated` | Public | Top 10 products by rating |
| `GET` | `/api/products/{id}` | Public | Product detail |
| `GET` | `/api/products/inactive` | Admin | Search inactive products |
| `POST` | `/api/products` | Admin | Create product |
| `PUT` | `/api/products/{id}` | Admin | Update product |
| `DELETE` | `/api/products/{id}` | Admin | Soft-delete product |
| `PUT` | `/api/products/{id}/reactivate` | Admin | Restore product |

Query parameters for `GET /api/products`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `nombre` | `string` | — | Partial name match |
| `brandId` | `long` | — | Filter by brand ID |
| `sortBy` | `string` | `createdAt` | `name` · `price` · `rating` · `createdAt` |
| `sort` | `string` | `desc` | `asc` · `desc` |
| `page` | `int` | `0` | Page number |
| `size` | `int` | `20` | Results per page |

</details>

<details>
<summary><strong>Product Images</strong> — <code>/api/products/{productId}/images</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/products/{productId}/images` | Public | All images for a product, sorted by `orden` |
| `POST` | `/api/products/{productId}/images` | Admin | Upload image to Cloudinary. Params: `file` (multipart), `orden` (int), `descripcion` (optional) |
| `GET` | `/api/products/images/{imageId}` | Public | Single image by ID |
| `PUT` | `/api/products/images/{imageId}` | Admin | Update `url`, `orden`, or `descripcion` |
| `DELETE` | `/api/products/images/{imageId}` | Admin | Delete image from DB and Cloudinary |
| `DELETE` | `/api/products/{productId}/images` | Admin | Delete all images for a product from DB and Cloudinary |

</details>

<details>
<summary><strong>Cart</strong> — <code>/api/cart</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/cart/user/{userId}` | Auth | Get user's cart |
| `POST` | `/api/cart/user/{userId}/items` | Auth | Add item to cart |
| `PUT` | `/api/cart/items/{cartItemId}` | Auth | Update item quantity |
| `DELETE` | `/api/cart/items/{cartItemId}` | Auth | Remove single item |
| `DELETE` | `/api/cart/user/{userId}/clear` | Auth | Clear entire cart |
| `GET` | `/api/cart/{cartId}/total` | Auth | Calculate cart total |

</details>

<details>
<summary><strong>Orders</strong> — <code>/api/orders</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/orders/user/{userId}` | Auth | Create order from cart |
| `GET` | `/api/orders` | Auth | All orders (paginated) |
| `GET` | `/api/orders/{orderId}` | Auth | Order by ID |
| `GET` | `/api/orders/user/{userId}` | Auth | Orders by user (paginated) |
| `GET` | `/api/orders/user/{userId}/active` | Auth | Active orders: PROCESANDO, ENVIADO, ENTREGADO |
| `GET` | `/api/orders/status/{estado}` | Admin | Filter orders by status |
| `PUT` | `/api/orders/{orderId}/status/{estado}` | Admin | Set order status manually |
| `POST` | `/api/orders/{orderId}/cancel` | Auth | Cancel order |
| `POST` | `/api/orders/expire` | Admin | Manually trigger order expiration |
| `GET` | `/api/orders/user/{userId}/product/{productId}/purchased` | Auth | Check if user has a delivered order containing this product. Returns `purchased`, `hasReview`, `reviewId` |
| `GET` | `/api/orders/user/{userId}/reviewable-products` | Auth | Products from delivered orders, with review status (paginated) |

**Admin order actions — `/api/admin/orders`**

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/admin/orders/{orderId}/ship` | Admin | Advance order from PROCESANDO to ENVIADO |
| `POST` | `/api/admin/orders/{orderId}/deliver` | Admin | Advance order from ENVIADO to ENTREGADO |

</details>

<details>
<summary><strong>Payments</strong> — <code>/api/payments</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/payments/create` | Admin | Create Stripe PaymentIntent and local payment record |
| `GET` | `/api/payments/{paymentId}` | Admin | Payment by internal ID |
| `GET` | `/api/payments/stripe/{stripePaymentId}` | Admin | Payment by Stripe PaymentIntent ID |
| `PUT` | `/api/payments/{paymentId}/status/{estado}` | Admin | Update payment status |
| `PUT` | `/api/payments/{paymentId}/stripe-id` | Admin | Associate a Stripe ID to a payment |
| `GET` | `/api/payments/{paymentId}/sync` | Admin | Sync local status against Stripe |
| `POST` | `/api/payments/{paymentId}/process` | Admin | Confirm PaymentIntent on Stripe |
| `POST` | `/api/payments/{paymentId}/cancel` | Admin | Cancel PaymentIntent on Stripe |
| `POST` | `/api/payments/{paymentId}/simulate` | Admin | Simulate outcome in test mode. Param: `success=true/false` |

**Stripe webhook — `/api/stripe/webhook`**

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/stripe/webhook` | Public (signature-verified) | Handles `payment_intent.succeeded` and `payment_intent.payment_failed`. Updates payment and order status, restores stock on failure |

</details>

<details>
<summary><strong>Reviews</strong> — <code>/api/reviews</code></summary>

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/reviews?userId=` | Auth | Create review. Requires a delivered order containing the product |
| `GET` | `/api/reviews/product/{productId}` | Public | Paginated reviews for a product |
| `GET` | `/api/reviews/product/{productId}/user/{userId}` | Auth | Single review by user for a product |
| `GET` | `/api/reviews/product/{productId}/average` | Public | Average rating for a product |
| `PUT` | `/api/reviews/{reviewId}` | Auth | Edit review |
| `DELETE` | `/api/reviews/{reviewId}` | Auth | Delete review |

</details>

<details>
<summary><strong>Health</strong></summary>

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Service status with timestamp |
| `GET` | `/health` | Minimal status response for load balancers |

</details>

---

## Trying It Out

The app is live and open for testing. Create an account at [laptoplace.vercel.app](https://laptoplace.vercel.app/) and go through the full purchase flow.

> **Response times:** The backend is hosted on Render's free tier, which runs on shared, resource-limited infrastructure. Some requests may feel slower than expected — this is a hosting constraint, not a reflection of query performance or application efficiency.

**Payments run in Stripe test mode.** No real charges are made. This is not a real business, but the integration uses Stripe's production-grade PaymentIntent API and is fully scalable to live payments — switching to real cards requires only swapping the API keys.

Use the following card to simulate a successful payment:

```
Number:   4242 4242 4242 4242
Expiry:   any future date
CVC:      any 3 digits
```

To test declined transactions and other scenarios (insufficient funds, authentication required, stolen card, etc.), refer to Stripe's full list of test cards at [docs.stripe.com/testing](https://docs.stripe.com/testing).

> **Password reset emails may land in spam.** The sending domain is not a custom domain, which affects deliverability and can cause some providers to flag these messages. If the reset email does not appear in your inbox, check your spam folder.

---

## Scheduled Jobs

Both schedulers run every 5 minutes.

| Scheduler | Trigger | Description |
|---|---|---|
| `OrderExpirationScheduler` | Every 5 min | Finds all orders in `PENDIENTE_PAGO` whose `expiresAt` has passed and marks them `EXPIRADO` |
| `OrderProgressionScheduler` | Every 5 min | Simulates logistics progression: `PROCESANDO → ENVIADO → ENTREGADO`. Processes `ENVIADO → ENTREGADO` first, then `PROCESANDO → ENVIADO`, so a single order cannot skip two states in one cycle |

Since there is no real shipping infrastructure, status progression is handled automatically to allow full testing of the order lifecycle.

---

## Running Locally

**Requirements:** Java 21+ · Node.js 18+ · PostgreSQL 15+

**Backend**

Create a `.env` file at the project root with the required variables (database URL, JWT secret, Stripe keys, Cloudinary URL, Brevo API key, and frontend URL), then:

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend**

```bash
cd frontend
npm install
ng serve
```

---

## Testing

Integration and unit tests cover all controllers, security configuration, Stripe integration, rate limiting, and scheduler behavior.

```bash
cd backend
./mvnw test
```

Test files are in `backend/src/test/java/com/laptophub/backend/`.
