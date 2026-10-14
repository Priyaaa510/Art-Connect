import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { mediaUrl, request, useApi } from "../api.js";
import { useAuth } from "../auth.jsx";
import { useCart } from "../cart.jsx";
import { Countdown, EmptyState, PageState } from "../components.jsx";
import { formatPrice } from "../format.js";
import { usePayment } from "../payment.jsx";

const BILLING_KEY = "artconnect.billing";

const INDIAN_STATES = [
  "Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chandigarh",
  "Chhattisgarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Goa", "Gujarat", "Haryana",
  "Himachal Pradesh", "Jammu and Kashmir", "Jharkhand", "Karnataka", "Kerala", "Ladakh", "Lakshadweep",
  "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Puducherry",
  "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand",
  "West Bengal",
];

const EMPTY_ADDRESS = { fullName: "", line1: "", line2: "", city: "", state: "", postalCode: "" };

/** The email and address used last time, so returning buyers don't retype them. */
function loadSavedBilling() {
  try {
    const saved = JSON.parse(localStorage.getItem(BILLING_KEY)) ?? {};
    return { email: saved.email ?? "", address: { ...EMPTY_ADDRESS, ...saved.address } };
  } catch {
    return { email: "", address: EMPTY_ADDRESS };
  }
}

function saveBilling(email, address) {
  try {
    localStorage.setItem(BILLING_KEY, JSON.stringify({ email, address }));
  } catch {
    // Not remembering the details is harmless.
  }
}

export function CartPage() {
  const { user } = useAuth();
  const { cart, loading, remove, reload } = useCart();
  const [error, setError] = useState("");
  const [removing, setRemoving] = useState(null);
  const navigate = useNavigate();

  // Availability can change while the cart sits there, so refresh it when the page opens
  useEffect(() => {
    reload();
  }, [reload]);

  async function removeItem(artworkId) {
    setRemoving(artworkId);
    setError("");
    try {
      await remove(artworkId);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setRemoving(null);
    }
  }

  if (!user) {
    return (
      <section className="container page-section">
        <h2>Your cart</h2>
        <EmptyState><Link to="/login" state={{ from: "/cart" }}>Log in</Link> to see your cart.</EmptyState>
      </section>
    );
  }

  const unavailable = cart.items.filter((item) => !item.available);

  return (
    <section className="container page-section">
      <h2 className="mb-1">Your cart</h2>
      <p className="lead-muted">
        {cart.items.length ? `${cart.items.length} ${cart.items.length === 1 ? "artwork" : "artworks"}` : "Nothing here yet."}
      </p>
      {error && <p className="text-danger" role="alert">{error}</p>}

      <PageState loading={loading && !cart.items.length} error="">
        {cart.items.length ? (
          <div className="cart-layout">
            <div className="order-list">
              {cart.items.map(({ artwork, available, problem }) => (
                <article key={artwork.id} className={`order-card glass-panel cart-item ${available ? "" : "unavailable"}`}>
                  <img src={mediaUrl(artwork.imageUrl)} alt="" />
                  <div className="order-details">
                    <h3><Link to={`/artworks/${artwork.id}`}>{artwork.title}</Link></h3>
                    <p>by <Link to={`/artists/${artwork.artistId}`}>{artwork.artistName}</Link> · {artwork.category}</p>
                    {artwork.medium && <p>{artwork.medium}{artwork.dimensions && <> · {artwork.dimensions}</>}</p>}
                    {!available && <p className="cart-problem"><i className="fas fa-circle-exclamation" aria-hidden="true" /> {problem}</p>}
                  </div>
                  <div className="order-actions">
                    <p className="order-price mb-0">{formatPrice(artwork.price)}</p>
                    <button type="button" className="btn btn-link btn-sm text-danger" disabled={removing === artwork.id}
                      onClick={() => removeItem(artwork.id)}>
                      <i className="fas fa-trash-can" aria-hidden="true" /> {removing === artwork.id ? "Removing..." : "Remove"}
                    </button>
                  </div>
                </article>
              ))}
            </div>

            <aside className="glass-panel order-summary">
              <h3>Order summary</h3>
              <dl>
                <dt>Subtotal ({cart.availableCount} {cart.availableCount === 1 ? "item" : "items"})</dt><dd>{formatPrice(cart.total)}</dd>
                <dt>Shipping</dt><dd>Free</dd>
              </dl>
              <div className="summary-total"><span>Total</span><strong>{formatPrice(cart.total)}</strong></div>
              {unavailable.length > 0 && (
                <p className="cart-problem small">
                  {unavailable.length === 1 ? "1 artwork isn't" : `${unavailable.length} artworks aren't`} available
                  and won&rsquo;t be included. You can remove {unavailable.length === 1 ? "it" : "them"} from your cart.
                </p>
              )}
              <button type="button" className="btn btn-primary btn-lg w-100" disabled={!cart.availableCount}
                onClick={() => navigate("/checkout")}>
                Proceed to checkout
              </button>
              <Link to="/gallery" className="btn btn-link w-100">Continue shopping</Link>
            </aside>
          </div>
        ) : (
          <EmptyState>Your cart is empty. <Link to="/gallery">Discover original art</Link>.</EmptyState>
        )}
      </PageState>
    </section>
  );
}

/**
 * Shipping address and billing email, then payment. Checks out the available items in the cart,
 * or with ?order=ID a won auction.
 */
export function CheckoutPage() {
  const { user } = useAuth();
  const cart = useCart();
  const [params] = useSearchParams();
  const auctionOrderId = params.get("order");
  const orders = useApi(auctionOrderId ? "/api/me/orders" : null);
  const { openPayment, paymentDialog } = usePayment();

  const saved = useMemo(loadSavedBilling, []);
  const [email, setEmail] = useState(saved.email);
  const [address, setAddress] = useState(() => ({ ...saved.address, fullName: saved.address.fullName || user?.displayName || "" }));
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  // Items being paid for, frozen once payment starts (they then show as reserved in the cart)
  const [frozenItems, setFrozenItems] = useState(null);
  const [pending, setPending] = useState(null); // an unpaid checkout after the payment window was closed
  const [paid, setPaid] = useState(null);

  const auctionOrder = orders.data?.find((order) => String(order.id) === auctionOrderId);
  const items = frozenItems ?? (auctionOrderId
    ? (auctionOrder && auctionOrder.status === "AWAITING_PAYMENT"
      ? [{ id: auctionOrder.artworkId, title: auctionOrder.artworkTitle, artistName: auctionOrder.artistName,
        imageUrl: auctionOrder.imageUrl, price: auctionOrder.price, auction: true }]
      : [])
    : cart.cart.items.filter((item) => item.available).map(({ artwork }) => ({
      id: artwork.id, title: artwork.title, artistName: artwork.artistName, imageUrl: artwork.imageUrl, price: artwork.price,
    })));
  const total = items.reduce((sum, item) => sum + Number(item.price), 0);

  function update(field, value) {
    if (field === "postalCode") value = value.replace(/\D/g, "").slice(0, 6);
    setAddress((current) => ({ ...current, [field]: value }));
  }

  async function pay(start) {
    setBusy(true);
    setError("");
    const result = await openPayment(start);
    if (result.status === "paid") {
      setPaid(result.checkout);
      setPending(null);
      cart.reload();
    } else if (result.checkoutId) {
      const details = await request(`/api/checkouts/${result.checkoutId}`).catch(() => null);
      setPending(details?.status === "AWAITING_PAYMENT" ? details : null);
      if (details?.status === "PAID") setPaid(details);
      cart.reload();
    }
    setBusy(false);
  }

  async function submit(event) {
    event.preventDefault();
    if (!/^[1-9][0-9]{5}$/.test(address.postalCode)) {
      setError("Enter a valid 6-digit PIN code.");
      return;
    }
    const body = {
      email: email.trim(),
      shippingAddress: { ...address, line2: address.line2.trim() || null },
      ...(auctionOrderId ? { orderId: Number(auctionOrderId) } : { artworkIds: items.map((item) => item.id) }),
    };
    saveBilling(body.email, address);
    setFrozenItems(items);
    const previous = pending;
    await pay(async () => {
      // Details may have changed since the payment window was closed: replace the earlier unpaid checkout
      if (previous && !auctionOrderId) {
        await request(`/api/checkouts/${previous.id}/cancel`, { method: "POST" }).catch(() => null);
      }
      return request("/api/checkouts", { method: "POST", json: body });
    });
  }

  if (paid) {
    return (
      <section className="container page-section">
        <div className="glass-panel checkout-done">
          <span className="checkout-tick" aria-hidden="true"><i className="fas fa-check" /></span>
          <h2>Thank you for your order!</h2>
          <p>
            We&rsquo;ve received {formatPrice(paid.total)} for {paid.items.length === 1 ? `"${paid.items[0].artworkTitle}"` : `${paid.items.length} artworks`}.
            Invoice <strong>{paid.invoiceNumber}</strong> is on its way to <strong>{paid.billingEmail}</strong>.
          </p>
          {paid.shippingAddress && <p className="mb-4">Shipping to {paid.shippingAddress.fullName}, {paid.shippingAddress.city}, {paid.shippingAddress.state} {paid.shippingAddress.postalCode}.</p>}
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <Link to={`/checkouts/${paid.id}/invoice`} className="btn btn-primary">View invoice</Link>
            <Link to="/collection" className="btn btn-outline-dark">My orders</Link>
            <Link to="/gallery" className="btn btn-link">Continue shopping</Link>
          </div>
        </div>
      </section>
    );
  }

  const loadingItems = auctionOrderId ? orders.loading : cart.loading && !cart.cart.items.length;

  return (
    <section className="container page-section">
      <h2 className="mb-1">Checkout</h2>
      <p className="lead-muted">Where should we send your art?</p>
      {paymentDialog}

      <PageState loading={loadingItems} error={orders.error}>
        {items.length === 0 ? (
          <EmptyState>
            {auctionOrderId ? "This order has already been paid or cancelled." : "There's nothing in your cart to check out."}{" "}
            <Link to={auctionOrderId ? "/collection" : "/gallery"}>{auctionOrderId ? "See my orders" : "Discover original art"}</Link>.
          </EmptyState>
        ) : (
          <div className="cart-layout">
            <form className="glass-panel checkout-form" onSubmit={submit}>
              {pending && (
                <div className="checkout-pending" role="status">
                  <p className="mb-2">
                    <strong>Payment not completed.</strong>
                    {pending.holdExpiresAt
                      ? <> Your artworks are reserved for <Countdown endsAt={pending.holdExpiresAt} />.</>
                      : <> Your order is waiting for payment.</>}
                  </p>
                  <button type="button" className="btn btn-primary btn-sm" disabled={busy}
                    onClick={() => pay(() => request(`/api/checkouts/${pending.id}/pay`, { method: "POST" }))}>
                    Pay {formatPrice(pending.total)} now
                  </button>
                </div>
              )}

              <h3>Shipping address</h3>
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label" htmlFor="ship-name">Full name</label>
                  <input id="ship-name" className="form-control" required maxLength={100} autoComplete="name"
                    value={address.fullName} onChange={(event) => update("fullName", event.target.value)} />
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="ship-line1">Address</label>
                  <input id="ship-line1" className="form-control" required maxLength={200} autoComplete="address-line1"
                    placeholder="House / flat no., building, street" value={address.line1}
                    onChange={(event) => update("line1", event.target.value)} />
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="ship-line2">Area, landmark <span className="text-muted">(optional)</span></label>
                  <input id="ship-line2" className="form-control" maxLength={200} autoComplete="address-line2"
                    value={address.line2} onChange={(event) => update("line2", event.target.value)} />
                </div>
                <div className="col-md-5">
                  <label className="form-label" htmlFor="ship-city">City</label>
                  <input id="ship-city" className="form-control" required maxLength={100} autoComplete="address-level2"
                    value={address.city} onChange={(event) => update("city", event.target.value)} />
                </div>
                <div className="col-md-4">
                  <label className="form-label" htmlFor="ship-state">State</label>
                  <select id="ship-state" className="form-select" required autoComplete="address-level1"
                    value={address.state} onChange={(event) => update("state", event.target.value)}>
                    <option value="">Choose…</option>
                    {INDIAN_STATES.map((state) => <option key={state}>{state}</option>)}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label" htmlFor="ship-pin">PIN code</label>
                  <input id="ship-pin" className="form-control" required inputMode="numeric" autoComplete="postal-code"
                    pattern="[1-9][0-9]{5}" title="6-digit PIN code" value={address.postalCode}
                    onChange={(event) => update("postalCode", event.target.value)} />
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="ship-country">Country</label>
                  <input id="ship-country" className="form-control" value="India" disabled />
                </div>
              </div>

              <h3 className="mt-4">Contact</h3>
              <label className="form-label" htmlFor="checkout-email">Email</label>
              <input id="checkout-email" className="form-control" type="email" required autoComplete="email"
                placeholder="you@example.com" value={email} onChange={(event) => setEmail(event.target.value)} />
              <p className="form-text">Your invoice will be emailed to this address.</p>

              {error && <p className="text-danger" role="alert">{error}</p>}
              <button type="submit" className="btn btn-primary btn-lg w-100 mt-2" disabled={busy}>
                {busy ? "Please wait..." : `${pending ? "Update details and pay" : "Continue to payment"} · ${formatPrice(total)}`}
              </button>
              <p className="checkout-secure"><i className="fas fa-lock" aria-hidden="true" /> Secure payment · UPI, cards, netbanking &amp; wallets</p>
            </form>

            <aside className="glass-panel order-summary">
              <h3>Order summary</h3>
              <ul className="summary-items">
                {items.map((item) => (
                  <li key={item.id}>
                    <img src={mediaUrl(item.imageUrl)} alt="" />
                    <span>
                      <strong>{item.title}</strong>
                      <small>{item.auction ? "Won at auction" : `by ${item.artistName}`}</small>
                    </span>
                    <span>{formatPrice(item.price)}</span>
                  </li>
                ))}
              </ul>
              <dl>
                <dt>Subtotal</dt><dd>{formatPrice(total)}</dd>
                <dt>Shipping</dt><dd>Free</dd>
              </dl>
              <div className="summary-total"><span>Total</span><strong>{formatPrice(total)}</strong></div>
              {!auctionOrderId && !frozenItems && <Link to="/cart" className="btn btn-link w-100">Edit cart</Link>}
            </aside>
          </div>
        )}
      </PageState>
    </section>
  );
}
