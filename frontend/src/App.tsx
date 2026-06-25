import {
  ArrowLeft,
  ArrowRight,
  Boxes,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Edit,
  FolderPlus,
  History,
  LogOut,
  Package,
  Plus,
  Search,
  ShoppingCart,
  Trash2,
  User,
  Warehouse
} from 'lucide-react';
import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import {
  Link,
  NavLink,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useLocation,
  useParams
} from 'react-router-dom';
import { ApiError, api, getStoredAuth, setStoredAuth } from './api';
import type {
  AuthResponse,
  Cart,
  Category,
  Order,
  Product,
  ProductPayload,
  UserProfile
} from './types';

const money = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD'
});

const dateTime = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
});

function formatMoney(value: number) {
  return money.format(Number(value || 0));
}

function formatDate(value: string) {
  return dateTime.format(new Date(value));
}

function getErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Unexpected error';
}

function categoryName(categories: Category[], id: number) {
  return categories.find((category) => category.id === id)?.name || `Category ${id}`;
}

function productTotal(product: Product | undefined, quantity: number) {
  return Number(product?.price || 0) * quantity;
}

function AppShell({
  auth,
  onLogout,
  children
}: {
  auth: AuthResponse | null;
  onLogout: () => void;
  children: ReactNode;
}) {
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    navigate(`/?q=${encodeURIComponent(search.trim())}`);
  };

  return (
    <div className="app-shell">
      <header className="site-header">
        <Link className="brand" to="/">
          Cool Online Shop
        </Link>
        <nav className="main-nav" aria-label="Main navigation">
          <NavLink to="/">Catalog</NavLink>
          <NavLink to="/cart">Cart</NavLink>
          {auth?.role === 'ADMIN' ? <NavLink to="/admin/products">Admin</NavLink> : null}
        </nav>
        <form className="search-box" onSubmit={submitSearch}>
          <Search size={20} />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search appliances..."
            aria-label="Search products"
          />
        </form>
        <div className="header-actions">
          {auth ? (
            <>
              <NavLink className="icon-link" to="/profile" title="Profile">
                <User size={20} />
                <span>Profile</span>
              </NavLink>
              <button className="ghost-icon" type="button" onClick={onLogout} title="Logout">
                <LogOut size={20} />
              </button>
            </>
          ) : (
            <NavLink className="icon-link" to="/login">
              <User size={20} />
              <span>Login</span>
            </NavLink>
          )}
        </div>
      </header>
      <main>{children}</main>
      <Footer />
    </div>
  );
}

function Footer() {
  return (
    <footer className="site-footer">
      <span>Cool Online Shop</span>
      <span>Contact: support@coolshop.com</span>
      <span>© 2024 Cool Online Shop. All rights reserved.</span>
    </footer>
  );
}

function ProtectedRoute({ auth, children }: { auth: AuthResponse | null; children: ReactNode }) {
  if (!auth) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function AdminRoute({ auth, children }: { auth: AuthResponse | null; children: ReactNode }) {
  if (!auth) {
    return <Navigate to="/login" replace />;
  }
  if (auth.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function ProductArt({ product, compact = false }: { product: Product; compact?: boolean }) {
  const family = product.id % 4;
  return (
    <div className={`product-art art-${family} ${compact ? 'compact' : ''}`} aria-hidden="true">
      <div className="appliance-body">
        <div className="appliance-display" />
        <div className="appliance-line" />
        <div className="appliance-handle left" />
        <div className="appliance-handle right" />
      </div>
    </div>
  );
}

function EmptyState({
  icon,
  title,
  text,
  action
}: {
  icon: ReactNode;
  title: string;
  text: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      <h2>{title}</h2>
      <p>{text}</p>
      {action}
    </div>
  );
}

function Notice({ kind = 'info', children }: { kind?: 'info' | 'error' | 'success'; children: ReactNode }) {
  return <div className={`notice ${kind}`}>{children}</div>;
}

function CatalogPage({ auth }: { auth: AuthResponse | null }) {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState<number | 'all'>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const query = new URLSearchParams(location.search).get('q')?.toLowerCase() || '';

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [productPage, categoryList] = await Promise.all([
        api.getProducts(page, 12),
        api.getCategories()
      ]);
      setProducts(productPage.items);
      setTotalPages(productPage.totalPages);
      setCategories(categoryList);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const matchesCategory = selectedCategory === 'all' || product.categoryId === selectedCategory;
      const matchesQuery =
        !query ||
        product.name.toLowerCase().includes(query) ||
        (product.description || '').toLowerCase().includes(query);
      return matchesCategory && matchesQuery;
    });
  }, [products, query, selectedCategory]);

  const addToCart = async (product: Product) => {
    if (!auth) {
      navigate('/login');
      return;
    }
    await api.addCartItem(product.id, 1);
    navigate('/cart');
  };

  return (
    <section className="page">
      <div className="hero-band">
        <div>
          <p className="eyebrow">Reliable commerce</p>
          <h1>Find dependable appliances for everyday work.</h1>
          <p>
            Browse real catalog items, stock counts and prices from the catalog service.
          </p>
        </div>
        <div className="hero-product" aria-hidden="true">
          <div className="hero-fridge">
            <div />
            <div />
          </div>
        </div>
      </div>

      <div className="chips" aria-label="Category filters">
        <button
          className={selectedCategory === 'all' ? 'chip active' : 'chip'}
          type="button"
          onClick={() => setSelectedCategory('all')}
        >
          All
        </button>
        {categories.map((category) => (
          <button
            key={category.id}
            className={selectedCategory === category.id ? 'chip active' : 'chip'}
            type="button"
            onClick={() => setSelectedCategory(category.id)}
          >
            {category.name}
          </button>
        ))}
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}
      {loading ? <div className="grid-skeleton">Loading catalog...</div> : null}

      {!loading && filteredProducts.length === 0 ? (
        <EmptyState
          icon={<Package size={28} />}
          title="No products found"
          text="The catalog service did not return matching products for this view."
        />
      ) : null}

      <div className="product-grid">
        {filteredProducts.map((product) => (
          <article className="product-card" key={product.id}>
            <Link to={`/products/${product.id}`} className="product-art-link">
              <ProductArt product={product} />
            </Link>
            <div className="product-card-body">
              <Link to={`/products/${product.id}`} className="product-title">
                {product.name}
              </Link>
              <p>{product.description || 'No description provided.'}</p>
              <div className="card-meta">
                <strong>{formatMoney(product.price)}</strong>
                <span className={product.availableQuantity > 0 ? 'stock in' : 'stock out'}>
                  {product.availableQuantity > 0
                    ? `In Stock (${product.availableQuantity})`
                    : 'Out of Stock'}
                </span>
              </div>
              <button
                className="primary full"
                type="button"
                disabled={product.availableQuantity < 1}
                onClick={() => void addToCart(product)}
              >
                <ShoppingCart size={18} />
                Add to Cart
              </button>
            </div>
          </article>
        ))}
      </div>

      {totalPages > 1 ? (
        <div className="pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            <ChevronLeft size={18} />
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            <ChevronRight size={18} />
          </button>
        </div>
      ) : null}
    </section>
  );
}

function ProductDetailsPage({ auth }: { auth: AuthResponse | null }) {
  const { id } = useParams();
  const productId = Number(id);
  const [product, setProduct] = useState<Product | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [productData, categoryData] = await Promise.all([
          api.getProduct(productId),
          api.getCategories()
        ]);
        setProduct(productData);
        setCategories(categoryData);
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [productId]);

  const addToCart = async () => {
    if (!product) {
      return;
    }
    if (!auth) {
      navigate('/login');
      return;
    }
    await api.addCartItem(product.id, quantity);
    navigate('/cart');
  };

  if (loading) {
    return <section className="page">Loading product...</section>;
  }

  if (error || !product) {
    return (
      <section className="page">
        <Notice kind="error">{error || 'Product not found'}</Notice>
      </section>
    );
  }

  return (
    <section className="page product-detail">
      <Link className="back-link" to="/">
        <ArrowLeft size={18} />
        Back to catalog
      </Link>
      <div className="detail-grid">
        <div className="detail-media">
          <ProductArt product={product} />
        </div>
        <div className="detail-copy">
          <span className="pill">{categoryName(categories, product.categoryId)}</span>
          <h1>{product.name}</h1>
          <strong className="price-large">{formatMoney(product.price)}</strong>
          <div className="availability">
            <Check size={20} />
            <span>
              {product.availableQuantity > 0
                ? `Available Quantity: ${product.availableQuantity}`
                : 'Out of stock'}
            </span>
          </div>
          <p className="description">{product.description || 'No product description provided.'}</p>
          <div className="buy-row">
            <div className="stepper">
              <button
                type="button"
                onClick={() => setQuantity((value) => Math.max(1, value - 1))}
                aria-label="Decrease quantity"
              >
                −
              </button>
              <span>{quantity}</span>
              <button
                type="button"
                onClick={() =>
                  setQuantity((value) => Math.min(product.availableQuantity || 1, value + 1))
                }
                aria-label="Increase quantity"
              >
                +
              </button>
            </div>
            <button
              className="primary"
              type="button"
              disabled={product.availableQuantity < 1}
              onClick={() => void addToCart()}
            >
              <ShoppingCart size={20} />
              Add to Cart
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}

function useCartProducts() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [cartData, productPage] = await Promise.all([api.getCart(), api.getProducts(0, 100)]);
      setCart(cartData);
      setProducts(productPage.items);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const productById = useMemo(() => new Map(products.map((product) => [product.id, product])), [products]);
  const total = useMemo(
    () => cart?.items.reduce((sum, item) => sum + productTotal(productById.get(item.productId), item.quantity), 0) || 0,
    [cart, productById]
  );

  return { cart, productById, total, loading, error, reload: load };
}

function CartPage() {
  const { cart, productById, total, loading, error, reload } = useCartProducts();
  const navigate = useNavigate();

  const updateQuantity = async (productId: number, quantity: number) => {
    if (quantity < 1) {
      return;
    }
    await api.updateCartItem(productId, quantity);
    await reload();
  };

  const removeItem = async (productId: number) => {
    await api.deleteCartItem(productId);
    await reload();
  };

  const clearCart = async () => {
    await api.clearCart();
    await reload();
  };

  if (loading) {
    return <section className="page">Loading cart...</section>;
  }

  if (error) {
    return (
      <section className="page">
        <Notice kind="error">{error}</Notice>
      </section>
    );
  }

  const items = cart?.items || [];

  return (
    <section className="page">
      <div className="page-heading">
        <h1>Shopping Cart</h1>
        <p>You have {items.length} item{items.length === 1 ? '' : 's'} in your cart.</p>
      </div>
      {items.length === 0 ? (
        <EmptyState
          icon={<ShoppingCart size={28} />}
          title="Your cart is empty"
          text="Add available catalog products before checkout."
          action={
            <Link className="primary" to="/">
              Continue Shopping
            </Link>
          }
        />
      ) : (
        <div className="cart-layout">
          <div className="cart-list">
            {items.map((item) => {
              const product = productById.get(item.productId);
              return (
                <article className="cart-item" key={item.productId}>
                  {product ? <ProductArt product={product} compact /> : <div className="missing-art" />}
                  <div>
                    <h2>{product?.name || `Product ${item.productId}`}</h2>
                    <p>{product?.description || 'Product data is not available in the current page.'}</p>
                    <div className="stepper">
                      <button
                        type="button"
                        onClick={() => void updateQuantity(item.productId, item.quantity - 1)}
                      >
                        −
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        type="button"
                        onClick={() => void updateQuantity(item.productId, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <div className="cart-item-side">
                    <strong>{formatMoney(productTotal(product, item.quantity))}</strong>
                    <button className="danger-link" type="button" onClick={() => void removeItem(item.productId)}>
                      <Trash2 size={18} />
                      Remove
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
          <aside className="summary-panel">
            <h2>Order Summary</h2>
            <div className="summary-row">
              <span>Items</span>
              <span>{items.length}</span>
            </div>
            <div className="summary-row total">
              <span>Total</span>
              <strong>{formatMoney(total)}</strong>
            </div>
            <button className="primary full" type="button" onClick={() => navigate('/checkout')}>
              Proceed to Checkout
            </button>
            <button className="link-button" type="button" onClick={() => void clearCart()}>
              Clear Cart
            </button>
          </aside>
        </div>
      )}
    </section>
  );
}

function CheckoutPage() {
  const { cart, productById, total, loading, error } = useCartProducts();
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const navigate = useNavigate();
  const items = cart?.items || [];

  const placeOrder = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const order = await api.checkout();
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setSubmitError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <section className="page">Loading checkout...</section>;
  }

  return (
    <section className="page">
      <div className="page-heading">
        <h1>Checkout</h1>
        <p>Review your cart and create an order.</p>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      {submitError ? <Notice kind="error">{submitError}</Notice> : null}
      {items.length === 0 ? (
        <EmptyState
          icon={<CircleAlert size={28} />}
          title="Cart is empty"
          text="Checkout requires at least one cart item."
          action={<Link className="primary" to="/">Return to Catalog</Link>}
        />
      ) : (
        <div className="checkout-layout">
          <div className="panel">
            <h2>Order Items</h2>
            <div className="checkout-items">
              {items.map((item) => {
                const product = productById.get(item.productId);
                return (
                  <div className="checkout-item" key={item.productId}>
                    {product ? <ProductArt product={product} compact /> : <div className="missing-art" />}
                    <div>
                      <strong>{product?.name || `Product ${item.productId}`}</strong>
                      <span>Qty: {item.quantity}</span>
                    </div>
                    <strong>{formatMoney(productTotal(product, item.quantity))}</strong>
                  </div>
                );
              })}
            </div>
          </div>
          <aside className="summary-panel">
            <h2>Order Total</h2>
            <div className="summary-row total">
              <span>Total</span>
              <strong>{formatMoney(total)}</strong>
            </div>
            <button className="primary full" type="button" disabled={submitting} onClick={() => void placeOrder()}>
              {submitting ? 'Creating Order...' : 'Place Order'}
              <ArrowRight size={20} />
            </button>
          </aside>
        </div>
      )}
    </section>
  );
}

function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        setOrders(await api.getOrders());
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  if (loading) {
    return <section className="page">Loading orders...</section>;
  }

  return (
    <section className="page">
      <div className="page-heading">
        <h1>Order History</h1>
        <p>Review previous orders created from your cart.</p>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      {orders.length === 0 ? (
        <EmptyState icon={<History size={28} />} title="No orders yet" text="Created orders will appear here." />
      ) : (
        <div className="table-panel">
          <div className="table-row table-head">
            <span>Order ID & Date</span>
            <span>Items</span>
            <span>Status</span>
            <span>Total Amount</span>
            <span>Actions</span>
          </div>
          {orders.map((order) => (
            <div className="table-row" key={order.id}>
              <span>
                <strong>#{order.id}</strong>
                <small>{formatDate(order.createdAt)}</small>
              </span>
              <span>{order.items.length}</span>
              <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span>
              <strong>{formatMoney(order.totalAmount)}</strong>
              <Link className="secondary" to={`/orders/${order.id}`}>
                View Details
              </Link>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function OrderDetailsPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        setOrder(await api.getOrder(orderId));
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [orderId]);

  if (loading) {
    return <section className="page">Loading order...</section>;
  }

  if (error || !order) {
    return (
      <section className="page">
        <Notice kind="error">{error || 'Order not found'}</Notice>
      </section>
    );
  }

  return (
    <section className="page">
      <Link className="back-link" to="/orders">
        <ArrowLeft size={18} />
        Back to orders
      </Link>
      <div className="order-banner">
        <div>
          <h1>Order #{order.id}</h1>
          <p>Placed on {formatDate(order.createdAt)}</p>
        </div>
        <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span>
      </div>
      <div className="checkout-layout">
        <div className="panel">
          <h2>Order Items</h2>
          <div className="order-items-table">
            {order.items.map((item) => (
              <div className="order-line" key={item.id}>
                <span>
                  <strong>{item.productName}</strong>
                  <small>Product ID: {item.productId}</small>
                </span>
                <span>{formatMoney(item.productPrice)}</span>
                <span>Qty: {item.quantity}</span>
                <strong>{formatMoney(item.productPrice * item.quantity)}</strong>
              </div>
            ))}
          </div>
        </div>
        <aside className="summary-panel">
          <h2>Order Summary</h2>
          <div className="summary-row total">
            <span>Total</span>
            <strong>{formatMoney(order.totalAmount)}</strong>
          </div>
        </aside>
      </div>
    </section>
  );
}

function AuthPage({ mode, onAuth }: { mode: 'login' | 'register'; onAuth: (auth: AuthResponse) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (mode === 'register' && password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    setLoading(true);
    try {
      const auth = mode === 'login' ? await api.login(email, password) : await api.register(email, password);
      setStoredAuth(auth);
      onAuth(auth);
      navigate('/');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-screen">
      <Link className="auth-brand" to="/">
        <ShoppingCart size={30} />
        Cool Online Shop
      </Link>
      <form className="auth-card" onSubmit={(event) => void submit(event)}>
        <h1>{mode === 'login' ? 'Login' : 'Create Account'}</h1>
        {error ? <Notice kind="error">{error}</Notice> : null}
        <label>
          Email Address
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="name@example.com"
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password"
            minLength={mode === 'register' ? 8 : undefined}
            required
          />
        </label>
        {mode === 'register' ? (
          <label>
            Confirm Password
            <input
              type="password"
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
              placeholder="Confirm password"
              minLength={8}
              required
            />
          </label>
        ) : null}
        <button className="primary full" type="submit" disabled={loading}>
          {loading ? 'Please wait...' : mode === 'login' ? 'Login' : 'Register'}
          <ArrowRight size={20} />
        </button>
        <div className="auth-switch">
          {mode === 'login' ? (
            <>
              Need an account? <Link to="/register">Register</Link>
            </>
          ) : (
            <>
              Already have an account? <Link to="/login">Login</Link>
            </>
          )}
        </div>
      </form>
    </main>
  );
}

function ProfilePage({ auth }: { auth: AuthResponse }) {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [missingProfile, setMissingProfile] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.getProfile();
        setProfile(data);
        setFirstName(data.firstName);
        setLastName(data.lastName);
        setPhone(data.phone || '');
        setMissingProfile(false);
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          setMissingProfile(true);
        } else {
          setError(getErrorMessage(err));
        }
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    try {
      const payload = { firstName, lastName, phone };
      const saved = missingProfile ? await api.createProfile(payload) : await api.updateProfile(payload);
      setProfile(saved);
      setMissingProfile(false);
      setMessage('Profile saved');
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  if (loading) {
    return <section className="page">Loading profile...</section>;
  }

  return (
    <section className="page">
      <div className="page-heading">
        <h1>Account Settings</h1>
        <p>Manage personal information stored in user-service.</p>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      {message ? <Notice kind="success">{message}</Notice> : null}
      {missingProfile ? (
        <Notice>Create your user-service profile to complete account setup.</Notice>
      ) : null}
      <div className="profile-layout">
        <aside className="profile-menu">
          <NavLink to="/profile">
            <User size={20} />
            Profile
          </NavLink>
          <NavLink to="/orders">
            <History size={20} />
            My Orders
          </NavLink>
        </aside>
        <form className="panel profile-form" onSubmit={(event) => void save(event)}>
          <h2>Personal Information</h2>
          <label>
            Email Address
            <input value={profile?.email || auth.email} readOnly />
          </label>
          <div className="form-grid">
            <label>
              First Name
              <input value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
            </label>
            <label>
              Last Name
              <input value={lastName} onChange={(event) => setLastName(event.target.value)} required />
            </label>
          </div>
          <label>
            Phone Number
            <input value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={50} />
          </label>
          <div className="form-actions">
            <button className="primary" type="submit">
              Save Changes
              <Check size={18} />
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}

function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <section className="admin-shell">
      <aside className="admin-sidebar">
        <h2>Admin Panel</h2>
        <p>Inventory Control</p>
        <NavLink to="/admin/products">
          <Boxes size={20} />
          Manage Products
        </NavLink>
        <NavLink to="/admin/categories">
          <Warehouse size={20} />
          Manage Categories
        </NavLink>
        <NavLink to="/">
          <ArrowLeft size={20} />
          View Shop
        </NavLink>
      </aside>
      <div className="admin-content">{children}</div>
    </section>
  );
}

function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [productPage, categoryList] = await Promise.all([
        api.getProducts(page, 20),
        api.getCategories()
      ]);
      setProducts(productPage.items);
      setTotalPages(productPage.totalPages);
      setCategories(categoryList);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = products.filter((product) =>
    product.name.toLowerCase().includes(query.toLowerCase())
  );
  const totalUnits = products.reduce((sum, product) => sum + product.availableQuantity, 0);
  const lowStock = products.filter((product) => product.availableQuantity > 0 && product.availableQuantity <= 5).length;

  const remove = async (id: number) => {
    await api.deleteProduct(id);
    await load();
  };

  return (
    <AdminLayout>
      <div className="admin-heading">
        <div>
          <h1>Products</h1>
          <p>Oversee inventory and availability.</p>
        </div>
        <Link className="primary" to="/admin/products/new">
          <Plus size={20} />
          Add New Product
        </Link>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      <div className="stats-grid">
        <div className="stat-card">
          <span>Total Units</span>
          <strong>{totalUnits}</strong>
        </div>
        <div className="stat-card">
          <span>Active Listings</span>
          <strong>{products.length}</strong>
        </div>
        <div className="stat-card">
          <span>Low Stock</span>
          <strong>{lowStock}</strong>
        </div>
        <div className="stat-card">
          <span>Categories</span>
          <strong>{categories.length}</strong>
        </div>
      </div>
      <div className="table-panel">
        <div className="admin-toolbar">
          <div className="search-box static">
            <Search size={20} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search products..."
            />
          </div>
        </div>
        <div className="table-row table-head admin-products">
          <span>Name</span>
          <span>Price</span>
          <span>Category</span>
          <span>Quantity</span>
          <span>Status</span>
          <span>Actions</span>
        </div>
        {filtered.map((product) => (
          <div className="table-row admin-products" key={product.id}>
            <span className="product-cell">
              <ProductArt product={product} compact />
              <strong>{product.name}</strong>
            </span>
            <span>{formatMoney(product.price)}</span>
            <span>{categoryName(categories, product.categoryId)}</span>
            <span className={product.availableQuantity <= 5 ? 'low' : ''}>{product.availableQuantity}</span>
            <span className={product.availableQuantity > 0 ? 'stock in' : 'stock out'}>
              {product.availableQuantity > 0 ? 'Active' : 'Hidden'}
            </span>
            <span className="row-actions">
              <Link className="icon-button" to={`/admin/products/${product.id}/edit`} title="Edit product">
                <Edit size={18} />
              </Link>
              <button className="icon-button danger" type="button" onClick={() => void remove(product.id)} title="Delete product">
                <Trash2 size={18} />
              </button>
            </span>
          </div>
        ))}
      </div>
      {totalPages > 1 ? (
        <div className="pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            <ChevronLeft size={18} />
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            <ChevronRight size={18} />
          </button>
        </div>
      ) : null}
    </AdminLayout>
  );
}

function ProductFormPage() {
  const { id } = useParams();
  const productId = id ? Number(id) : null;
  const isEdit = productId !== null;
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState<ProductPayload>({
    name: '',
    description: '',
    price: 0,
    categoryId: 0,
    availableQuantity: 0
  });
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const categoryList = await api.getCategories();
        setCategories(categoryList);
        if (categoryList[0] && !isEdit) {
          setForm((value) => ({ ...value, categoryId: categoryList[0].id }));
        }
        if (isEdit && productId) {
          const product = await api.getProduct(productId);
          setForm({
            name: product.name,
            description: product.description || '',
            price: Number(product.price),
            categoryId: product.categoryId,
            availableQuantity: product.availableQuantity
          });
        }
      } catch (err) {
        setError(getErrorMessage(err));
      }
    };
    void load();
  }, [isEdit, productId]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      if (isEdit && productId) {
        await api.updateProduct(productId, form);
      } else {
        await api.createProduct(form);
      }
      navigate('/admin/products');
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <AdminLayout>
      <Link className="back-link" to="/admin/products">
        <ArrowLeft size={18} />
        Back to Products
      </Link>
      <div className="admin-heading">
        <div>
          <h1>{isEdit ? 'Edit Product' : 'Add Product'}</h1>
          <p>Set catalog fields supported by catalog-service.</p>
        </div>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      <form className="panel product-form" onSubmit={(event) => void submit(event)}>
        <label>
          Product Name
          <input
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            required
          />
        </label>
        <div className="form-grid">
          <label>
            Category
            <select
              value={form.categoryId}
              onChange={(event) => setForm({ ...form, categoryId: Number(event.target.value) })}
              required
            >
              {categories.map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Price (USD)
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={form.price}
              onChange={(event) => setForm({ ...form, price: Number(event.target.value) })}
              required
            />
          </label>
        </div>
        <label>
          Available Quantity
          <input
            type="number"
            min="0"
            value={form.availableQuantity}
            onChange={(event) => setForm({ ...form, availableQuantity: Number(event.target.value) })}
            required
          />
        </label>
        <label>
          Product Description
          <textarea
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            rows={7}
          />
        </label>
        <div className="form-actions">
          <Link className="secondary" to="/admin/products">
            Cancel
          </Link>
          <button className="primary" type="submit">
            Save Product
            <Check size={18} />
          </button>
        </div>
      </form>
    </AdminLayout>
  );
}

function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setCategories(await api.getCategories());
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const create = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      await api.createCategory(name, description);
      setName('');
      setDescription('');
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <AdminLayout>
      <div className="admin-heading">
        <div>
          <h1>Category Management</h1>
          <p>Create high-level catalog groupings.</p>
        </div>
      </div>
      {error ? <Notice kind="error">{error}</Notice> : null}
      <div className="category-admin-grid">
        <div className="panel">
          <div className="panel-title-row">
            <h2>Existing Categories</h2>
            <span className="pill">{categories.length} Categories</span>
          </div>
          <div className="category-list">
            {categories.map((category) => (
              <div className="category-row" key={category.id}>
                <div className="category-icon">
                  <Warehouse size={20} />
                </div>
                <div>
                  <strong>{category.name}</strong>
                  <p>{category.description || 'No description provided.'}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
        <form className="panel category-form" onSubmit={(event) => void create(event)}>
          <h2>
            <FolderPlus size={24} />
            Add Category
          </h2>
          <label>
            Category Name
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            Description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={5}
            />
          </label>
          <button className="primary full" type="submit">
            Create Category
          </button>
        </form>
      </div>
    </AdminLayout>
  );
}

export function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => getStoredAuth());
  const navigate = useNavigate();

  const logout = () => {
    setStoredAuth(null);
    setAuth(null);
    navigate('/');
  };

  return (
    <Routes>
      <Route path="/login" element={<AuthPage mode="login" onAuth={setAuth} />} />
      <Route path="/register" element={<AuthPage mode="register" onAuth={setAuth} />} />
      <Route
        path="*"
        element={
          <AppShell auth={auth} onLogout={logout}>
            <Routes>
              <Route path="/" element={<CatalogPage auth={auth} />} />
              <Route path="/products/:id" element={<ProductDetailsPage auth={auth} />} />
              <Route
                path="/cart"
                element={
                  <ProtectedRoute auth={auth}>
                    <CartPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/checkout"
                element={
                  <ProtectedRoute auth={auth}>
                    <CheckoutPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute auth={auth}>
                    <OrdersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders/:id"
                element={
                  <ProtectedRoute auth={auth}>
                    <OrderDetailsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute auth={auth}>
                    {auth ? <ProfilePage auth={auth} /> : null}
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/products"
                element={
                  <AdminRoute auth={auth}>
                    <AdminProductsPage />
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/products/new"
                element={
                  <AdminRoute auth={auth}>
                    <ProductFormPage />
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/products/:id/edit"
                element={
                  <AdminRoute auth={auth}>
                    <ProductFormPage />
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/categories"
                element={
                  <AdminRoute auth={auth}>
                    <AdminCategoriesPage />
                  </AdminRoute>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </AppShell>
        }
      />
    </Routes>
  );
}
