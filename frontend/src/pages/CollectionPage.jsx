import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { mediaUrl, request, useApi } from "../api.js";
import { useCart } from "../cart.jsx";
import { Countdown, EmptyState, PageState } from "../components.jsx";
import { formatDate, formatPrice } from "../format.js";
import { paymentMethodLabel, usePayment } from "../payment.jsx";
import { OrderStatus } from "./StudioPages.jsx";

/** A collector's purchases and auction bids. */
export default function CollectionPage() {
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") ?? "orders";
  const orders = useApi("/api/me/orders");
  const bids = useApi("/api/me/bids");
  const cart = useCart();
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  const [payingId, setPayingId] = useState(null);
  const { openPayment, paymentDialog } = usePayment();

  /** Every order paid for together shares one checkout; paying or cancelling acts on all of them. */
  const checkoutSize = (order) => orders.data?.filter((item) => item.checkoutId === order.checkoutId).length ?? 1;

  async function pay(order) {
    setActionError("");
    setNotice("");
    setPayingId(order.checkoutId);
    const result = await openPayment(() => request(`/api/checkouts/${order.checkoutId}/pay`, { method: "POST" }));
    if (result.status === "paid") {
      setNotice(`Payment received. Invoice ${result.checkout.invoiceNumber} has been sent to ${result.checkout.billingEmail}.`);
      cart.reload();
    }
    orders.reload({ quiet: true });
    setPayingId(null);
  }

  async function cancel(order) {
    const others = checkoutSize(order) - 1;
    const what = others > 0 ? `"${order.artworkTitle}" and ${others} other ${others === 1 ? "artwork" : "artworks"} bought with it` : `"${order.artworkTitle}"`;
    if (!window.confirm(`Cancel your order for ${what}? ${others > 0 ? "They" : "It"} will go back on sale.`)) return;
    setActionError("");
    try {
      await request(`/api/orders/${order.id}/cancel`, { method: "POST" });
      orders.reload({ quiet: true });
      cart.reload();
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  return (
    <section className="container page-section">
      <h2 className="mb-1">My Collection</h2>
      <p className="lead-muted">Your purchases and auction activity.</p>
      <div className="filter-buttons justify-content-start" role="tablist">
        <button type="button" role="tab" aria-selected={tab === "orders"} className={`btn ${tab === "orders" ? "btn-primary" : "btn-outline-dark"}`} onClick={() => setParams({})}>Orders</button>
        <button type="button" role="tab" aria-selected={tab === "bids"} className={`btn ${tab === "bids" ? "btn-primary" : "btn-outline-dark"}`} onClick={() => setParams({ tab: "bids" })}>Bids</button>
      </div>
      {actionError && <p className="text-danger" role="alert">{actionError}</p>}
      {notice && <p className="text-success-light" role="status">{notice}</p>}
      {paymentDialog}

      {tab === "orders" && (
        <PageState loading={orders.loading} error={orders.error}>
          {orders.data?.length ? (
            <div className="order-list">
              {orders.data.map((order) => (
                <article key={order.id} className="order-card glass-panel">
                  <img src={mediaUrl(order.imageUrl)} alt="" />
                  <div className="order-details">
                    <h3>{order.artworkId ? <Link to={`/artworks/${order.artworkId}`}>{order.artworkTitle}</Link> : order.artworkTitle}</h3>
                    <p>by <Link to={`/artists/${order.artistId}`}>{order.artistName}</Link> · {order.source === "AUCTION" ? "Won at auction" : "Purchased"} {formatDate(order.createdAt)}</p>
                    <p className="order-price">{formatPrice(order.price)}</p>
                    {order.status === "AWAITING_PAYMENT" && order.holdExpiresAt && (
                      <p>Reserved for you for <Countdown endsAt={order.holdExpiresAt} /></p>
                    )}
                    {order.status === "AWAITING_PAYMENT" && order.checkoutId && checkoutSize(order) > 1 && (
                      <p>Checked out together with {checkoutSize(order) - 1} other {checkoutSize(order) === 2 ? "artwork" : "artworks"}</p>
                    )}
                    {order.shippingAddress && order.status !== "CANCELLED" && (
                      <p>Ship to {order.shippingAddress.fullName}, {order.shippingAddress.city} {order.shippingAddress.postalCode}</p>
                    )}
                    {order.invoiceNumber && (
                      <p>Invoice {order.invoiceNumber} · {paymentMethodLabel(order.paymentMethod)} · ref. <code>{order.paymentId}</code></p>
                    )}
                  </div>
                  <div className="order-actions">
                    <OrderStatus status={order.status} />
                    {order.invoiceNumber && (
                      <Link to={`/checkouts/${order.checkoutId}/invoice`} className="btn btn-outline-dark btn-sm">
                        <i className="fas fa-file-invoice" aria-hidden="true" /> Invoice
                      </Link>
                    )}
                    {order.status === "AWAITING_PAYMENT" && (
                      <>
                        {order.checkoutId ? (
                          <button type="button" className="btn btn-primary btn-sm" disabled={payingId !== null} onClick={() => pay(order)}>
                            {payingId === order.checkoutId ? "Processing..." : "Pay now"}
                          </button>
                        ) : (
                          <Link to={`/checkout?order=${order.id}`} className="btn btn-primary btn-sm">Checkout</Link>
                        )}
                        <button type="button" className="btn btn-outline-danger btn-sm" disabled={payingId !== null} onClick={() => cancel(order)}>Cancel</button>
                      </>
                    )}
                  </div>
                </article>
              ))}
            </div>
          ) : <EmptyState>No orders yet. <Link to="/gallery">Find something you love</Link>.</EmptyState>}
        </PageState>
      )}

      {tab === "bids" && (
        <PageState loading={bids.loading} error={bids.error}>
          {bids.data?.length ? (
            <div className="order-list">
              {bids.data.map(({ artwork, myHighestBid, leading, won }) => (
                <article key={artwork.id} className="order-card glass-panel">
                  <img src={mediaUrl(artwork.imageUrl)} alt="" />
                  <div className="order-details">
                    <h3><Link to={`/artworks/${artwork.id}`}>{artwork.title}</Link></h3>
                    <p>Your highest bid {formatPrice(myHighestBid)} · current {formatPrice(artwork.currentBid)}</p>
                    <p>{artwork.auctionLive ? <>Ends in <Countdown endsAt={artwork.auctionEndsAt} /></> : "Auction ended"}</p>
                  </div>
                  <div className="order-actions">
                    <BidStanding artwork={artwork} leading={leading} won={won} />
                    {artwork.auctionLive && !leading && <Link to={`/artworks/${artwork.id}`} className="btn btn-primary btn-sm">Bid again</Link>}
                  </div>
                </article>
              ))}
            </div>
          ) : <EmptyState>You haven't bid on anything yet. <Link to="/auctions">See live auctions</Link>.</EmptyState>}
        </PageState>
      )}
    </section>
  );
}

function BidStanding({ artwork, leading, won }) {
  if (won) return <span className="badge bg-success">Won</span>;
  if (artwork.auctionLive) {
    return leading ? <span className="badge bg-success">Leading</span> : <span className="badge bg-danger">Outbid</span>;
  }
  return leading ? <span className="badge bg-info text-dark">Settling</span> : <span className="badge bg-secondary">Not won</span>;
}
