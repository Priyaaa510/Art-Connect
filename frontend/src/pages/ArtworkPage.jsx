import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { mediaUrl, request, useApi } from "../api.js";
import { useAuth } from "../auth.jsx";
import { Avatar, Countdown, PageState, SaleBadge, Stars } from "../components.jsx";
import { formatDate, formatDateTime, formatPrice } from "../format.js";
import { useCart } from "../cart.jsx";

export default function ArtworkPage() {
  const { id } = useParams();
  const { data: artwork, setData: setArtwork, loading, error, reload } = useApi(`/api/artworks/${encodeURIComponent(id)}`);
  const bids = useApi(artwork?.saleType === "AUCTION" ? `/api/artworks/${encodeURIComponent(id)}/bids` : null);
  const reloadBids = bids.reload;

  // Keep a live auction fresh so other people's bids show up
  useEffect(() => {
    if (!artwork?.auctionLive) return undefined;
    const timer = setInterval(() => {
      reload({ quiet: true });
      reloadBids({ quiet: true });
    }, 15000);
    return () => clearInterval(timer);
  }, [artwork?.auctionLive, reload, reloadBids]);

  return (
    <section className="container page-section">
      <PageState loading={loading} error={error}>
        {artwork && (
          <>
            <div className="artwork-layout">
              <div className="artwork-image-panel">
                <img src={mediaUrl(artwork.imageUrl)} alt={artwork.title} className="details-image" />
              </div>
              <div className="artwork-info">
                <SaleBadge artwork={artwork} />
                <h1 className="artwork-title">{artwork.title}</h1>
                <Link to={`/artists/${artwork.artistId}`} className="artist-chip">
                  <Avatar src={artwork.artistAvatarUrl} name={artwork.artistName} size={32} />
                  <span>{artwork.artistName}</span>
                </Link>
                {artwork.reviewCount > 0 && <div className="mt-2"><Stars value={artwork.averageRating} count={artwork.reviewCount} /></div>}

                <dl className="artwork-facts">
                  <dt>Category</dt><dd><Link to={`/gallery?category=${encodeURIComponent(artwork.category)}`}>{artwork.category}</Link></dd>
                  {artwork.medium && <><dt>Medium</dt><dd>{artwork.medium}</dd></>}
                  {artwork.dimensions && <><dt>Dimensions</dt><dd>{artwork.dimensions}</dd></>}
                  {artwork.year && <><dt>Year</dt><dd>{artwork.year}</dd></>}
                  <dt>Listed</dt><dd>{formatDate(artwork.createdAt)}</dd>
                </dl>
                <p className="artwork-description">{artwork.description}</p>
                {artwork.tags.length > 0 && (
                  <div className="tag-list">
                    {artwork.tags.map((tag) => <Link key={tag} className="tag" to={`/gallery?q=${encodeURIComponent(tag)}`}>#{tag}</Link>)}
                  </div>
                )}

                {artwork.saleType === "AUCTION"
                  ? <AuctionPanel artwork={artwork} onBid={(updated) => { setArtwork(updated); reloadBids({ quiet: true }); }} />
                  : <PurchasePanel artwork={artwork} />}
              </div>
            </div>

            {artwork.saleType === "AUCTION" && (
              <div className="glass-panel mt-4">
                <h2 className="h4">Bid history</h2>
                <PageState loading={bids.loading} error={bids.error}>
                  {bids.data?.length ? (
                    <ol className="bid-history">
                      {bids.data.map((bid) => (
                        <li key={bid.id}>
                          <strong>{formatPrice(bid.amount)}</strong>
                          <span>{bid.bidderName}</span>
                          <time dateTime={bid.createdAt}>{formatDateTime(bid.createdAt)}</time>
                        </li>
                      ))}
                    </ol>
                  ) : <p className="mb-0">No bids yet. Be the first!</p>}
                </PageState>
              </div>
            )}

            <Reviews artwork={artwork} onChange={() => reload({ quiet: true })} />
          </>
        )}
      </PageState>
    </section>
  );
}

function useRequireLogin() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  return () => {
    if (user) return true;
    navigate("/login", { state: { from: location.pathname, message: "Log in to continue." } });
    return false;
  };
}

/** The signed-in user's unpaid order for this artwork, if they started checkout or won its auction. */
function useMyUnpaidOrder(artwork, user) {
  const orders = useApi(user && artwork.status === "RESERVED" ? "/api/me/orders" : null);
  return orders.data?.find((order) => order.artworkId === artwork.id && order.status === "AWAITING_PAYMENT");
}

function PurchasePanel({ artwork }) {
  const { user } = useAuth();
  const cart = useCart();
  const requireLogin = useRequireLogin();
  const myUnpaidOrder = useMyUnpaidOrder(artwork, user);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const isOwner = user?.id === artwork.artistId;
  const inCart = cart.contains(artwork.id);

  async function addToCart() {
    if (!requireLogin()) return;
    setBusy(true);
    setMessage(null);
    try {
      await cart.add(artwork.id);
      setMessage({ ok: true, text: "Added to your cart." });
    } catch (requestError) {
      setMessage({ ok: false, text: requestError.message });
    } finally {
      setBusy(false);
    }
  }

  async function removeFromCart() {
    setBusy(true);
    setMessage(null);
    try {
      await cart.remove(artwork.id);
    } catch (requestError) {
      setMessage({ ok: false, text: requestError.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="purchase-panel">
      <div className="price-line">
        <span>{artwork.status === "SOLD" ? "Sold for" : "Price"}</span>
        <strong>{formatPrice(artwork.price)}</strong>
      </div>
      {isOwner ? (
        <OwnerActions artwork={artwork} />
      ) : myUnpaidOrder ? (
        <>
          <p>This artwork is reserved for you while you pay.</p>
          <Link to="/collection" className="btn btn-primary btn-lg w-100">Complete payment</Link>
        </>
      ) : artwork.status === "AVAILABLE" ? (
        inCart ? (
          <>
            <Link to="/cart" className="btn btn-primary btn-lg w-100">
              <i className="fas fa-bag-shopping" aria-hidden="true" /> In your cart · View cart
            </Link>
            <button type="button" className="btn btn-link btn-sm w-100 mt-1" onClick={removeFromCart} disabled={busy}>Remove from cart</button>
          </>
        ) : (
          <>
            <button className="btn btn-primary btn-lg w-100" type="button" onClick={addToCart} disabled={busy}>
              <i className="fas fa-cart-plus" aria-hidden="true" /> {busy ? "Adding..." : "Add to cart"}
            </button>
            <p className="payment-methods">Original artwork · Free shipping in India · UPI, cards, netbanking & wallets</p>
          </>
        )
      ) : artwork.status === "RESERVED" ? (
        <p className="mb-0">Reserved: another buyer is completing payment. If they don't, it will be back on sale shortly.</p>
      ) : (
        <p className="mb-0">This artwork has found a home.</p>
      )}
      {message && (
        <p className={`mt-3 mb-0 ${message.ok ? "text-success-light" : "text-danger"}`} role="status">
          {message.text} {message.ok && <><Link to="/cart">View cart</Link> · <Link to="/checkout">Checkout</Link></>}
        </p>
      )}
    </div>
  );
}

function AuctionPanel({ artwork, onBid }) {
  const { user } = useAuth();
  const requireLogin = useRequireLogin();
  const myUnpaidOrder = useMyUnpaidOrder(artwork, user);
  const [amount, setAmount] = useState("");
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
  const isOwner = user?.id === artwork.artistId;
  const isLeading = user && user.id === artwork.highestBidderId;

  async function placeBid(event) {
    event.preventDefault();
    if (!requireLogin()) return;
    setBusy(true);
    setMessage(null);
    try {
      const updated = await request(`/api/artworks/${artwork.id}/bids`, {
        method: "POST",
        json: { amount: Number(amount || artwork.minimumBid) },
      });
      setAmount("");
      setMessage({ ok: true, text: "Bid placed. You're the highest bidder!" });
      onBid(updated);
    } catch (requestError) {
      setMessage({ ok: false, text: requestError.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="purchase-panel">
      <div className="price-line">
        <span>{artwork.currentBid != null ? "Current bid" : "Starting bid"}</span>
        <strong>{formatPrice(artwork.currentBid ?? artwork.price)}</strong>
      </div>
      <p className="auction-meta">
        {artwork.bidCount} {artwork.bidCount === 1 ? "bid" : "bids"}
        {artwork.highestBidderName && <> · leading: {artwork.highestBidderName}</>}
        <br />
        {artwork.auctionLive
          ? <>Ends in <Countdown endsAt={artwork.auctionEndsAt} /> ({formatDateTime(artwork.auctionEndsAt)})</>
          : artwork.status === "SOLD"
            ? <>Won by {artwork.highestBidderName} for {formatPrice(artwork.currentBid)}</>
            : artwork.status === "RESERVED"
              ? <>Won by {artwork.highestBidderName} for {formatPrice(artwork.currentBid)}. Awaiting payment.</>
              : <>Auction ended {formatDateTime(artwork.auctionEndsAt)}. Results are being settled.</>}
      </p>

      {myUnpaidOrder && (
        <>
          <p className="text-success-light">🎉 You won this auction! Add your shipping address and pay to claim it.</p>
          <Link to={`/checkout?order=${myUnpaidOrder.id}`} className="btn btn-primary btn-lg w-100">
            Checkout · {formatPrice(myUnpaidOrder.price)}
          </Link>
        </>
      )}

      {isOwner ? (
        <OwnerActions artwork={artwork} />
      ) : artwork.auctionLive && (
        isLeading ? (
          <p className="text-success-light mb-0">You're the highest bidder. Check back before the auction ends in case someone outbids you.</p>
        ) : (
          <form className="bid-form" onSubmit={placeBid}>
            <label className="form-label" htmlFor="bid-amount">Your bid (minimum {formatPrice(artwork.minimumBid)})</label>
            <div className="input-group">
              <span className="input-group-text">₹</span>
              <input
                id="bid-amount"
                className="form-control"
                type="number"
                min={artwork.minimumBid}
                step="0.01"
                placeholder={String(artwork.minimumBid)}
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
              />
              <button className="btn btn-primary" type="submit" disabled={busy}>{busy ? "Bidding..." : "Place bid"}</button>
            </div>
          </form>
        )
      )}
      {message && <p className={`mt-3 mb-0 ${message.ok ? "text-success-light" : "text-danger"}`} role="status">{message.text}</p>}
    </div>
  );
}

function OwnerActions({ artwork }) {
  return (
    <div>
      <p className="mb-2">This is your artwork.</p>
      {artwork.status === "AVAILABLE" && <Link to={`/studio/artworks/${artwork.id}/edit`} className="btn btn-outline-dark btn-sm">Edit details</Link>}
    </div>
  );
}

function Reviews({ artwork, onChange }) {
  const { user } = useAuth();
  const reviews = useApi(`/api/artworks/${artwork.id}/reviews`);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const alreadyReviewed = reviews.data?.some((review) => review.authorId === user?.id);
  const canReview = user && user.id !== artwork.artistId && !alreadyReviewed;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await request(`/api/artworks/${artwork.id}/reviews`, { method: "POST", json: { rating, comment } });
      setComment("");
      setRating(5);
      await reviews.reload({ quiet: true });
      onChange();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  async function remove(reviewId) {
    if (!window.confirm("Delete your review?")) return;
    try {
      await request(`/api/reviews/${reviewId}`, { method: "DELETE" });
      await reviews.reload({ quiet: true });
      onChange();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <div className="glass-panel mt-4">
      <h2 className="h4">Reviews</h2>
      {canReview && (
        <form className="review-form" onSubmit={submit}>
          <fieldset className="star-input">
            <legend className="form-label">Your rating</legend>
            {[1, 2, 3, 4, 5].map((value) => (
              <label key={value} className={value <= rating ? "active" : ""}>
                <input type="radio" name="rating" value={value} checked={rating === value} onChange={() => setRating(value)} />
                <span aria-hidden="true">★</span>
                <span className="visually-hidden">{value} stars</span>
              </label>
            ))}
          </fieldset>
          <label className="form-label" htmlFor="review-comment">Your review</label>
          <textarea id="review-comment" className="form-control mb-2" rows="3" maxLength={1000} required value={comment} onChange={(event) => setComment(event.target.value)} />
          <button className="btn btn-primary" type="submit" disabled={busy}>{busy ? "Posting..." : "Post review"}</button>
        </form>
      )}
      {!user && <p><Link to="/login" state={{ from: `/artworks/${artwork.id}` }}>Log in</Link> to leave a review.</p>}
      {error && <p className="text-danger" role="alert">{error}</p>}
      <PageState loading={reviews.loading} error={reviews.error}>
        {reviews.data?.length ? (
          <ul className="review-list">
            {reviews.data.map((review) => (
              <li key={review.id}>
                <div className="review-head">
                  <Avatar src={review.authorAvatarUrl} name={review.authorName} size={32} />
                  <strong>{review.authorName}</strong>
                  <Stars value={review.rating} />
                  <time dateTime={review.createdAt}>{formatDate(review.createdAt)}</time>
                  {(review.authorId === user?.id || user?.role === "ADMIN") && (
                    <button type="button" className="btn btn-link btn-sm text-danger ms-auto" onClick={() => remove(review.id)}>Delete</button>
                  )}
                </div>
                <p>{review.comment}</p>
              </li>
            ))}
          </ul>
        ) : <p className="mb-0">No reviews yet.</p>}
      </PageState>
    </div>
  );
}
