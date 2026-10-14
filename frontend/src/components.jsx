import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { mediaUrl } from "./api.js";
import { useAuth } from "./auth.jsx";
import { useCart } from "./cart.jsx";
import { formatPrice, timeLeft, useNow } from "./format.js";

/** Category shortcuts in the header, as in a gallery's "shop by medium" menu. */
const MENU_CATEGORIES = [
  { label: "Paintings", category: "Painting" },
  { label: "Photography", category: "Photography" },
  { label: "Drawings", category: "Drawing" },
  { label: "Sculpture", category: "Sculpture" },
  { label: "Prints", category: "Printmaking" },
  { label: "Textile Art", category: "Textile Art" },
  { label: "Digital Art", category: "Digital Art" },
];

export function Header() {
  const { user, isArtist, logout } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const [query, setQuery] = useState(params.get("q") ?? "");
  const activeCategory = location.pathname === "/gallery" ? params.get("category") : null;

  useEffect(() => {
    setQuery(new URLSearchParams(location.search).get("q") ?? "");
  }, [location.search]);

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  function search(event) {
    event.preventDefault();
    const text = query.trim();
    navigate(text ? `/gallery?q=${encodeURIComponent(text)}` : "/gallery");
  }

  return (
    <header className="site-header">
      <p className="announcement">
        Original art from independent artists · Secure checkout with UPI, cards &amp; netbanking
      </p>
      <div className="header-main">
        <Link className="brand" to="/">ArtConnect</Link>
        <form className="header-search" role="search" onSubmit={search}>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search artworks, artists, styles"
            aria-label="Search artworks"
          />
          <button type="submit" aria-label="Search"><i className="fas fa-magnifying-glass" aria-hidden="true" /></button>
        </form>
        <nav className="header-account" aria-label="Account">
          {user ? (
            <>
              {isArtist && <NavLink to="/studio">Studio</NavLink>}
              <NavLink to="/collection">My Collection</NavLink>
              <NavLink className="nav-user" to="/profile" title="Edit profile">
                <Avatar src={user.avatarUrl} name={user.displayName} size={28} />
                <span className="nav-user-name">{user.displayName}</span>
              </NavLink>
              <button type="button" className="link-button" onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <NavLink to="/login">Log in</NavLink>
              <Link className="btn btn-primary btn-sm" to="/register">Join</Link>
            </>
          )}
          <NavLink to="/cart" className="nav-cart" aria-label={count ? `Cart, ${count} ${count === 1 ? "item" : "items"}` : "Cart"}>
            <i className="fas fa-bag-shopping" aria-hidden="true" />
            {count > 0 && <span className="cart-badge">{count}</span>}
          </NavLink>
        </nav>
      </div>
      <nav className="category-nav" aria-label="Browse">
        <div className="category-nav-inner">
          <NavLink to="/gallery" end className={() => (location.pathname === "/gallery" && !activeCategory ? "active" : "")}>
            All Art
          </NavLink>
          {MENU_CATEGORIES.map((item) => (
            <Link
              key={item.category}
              to={`/gallery?category=${encodeURIComponent(item.category)}`}
              className={activeCategory === item.category ? "active" : ""}
            >
              {item.label}
            </Link>
          ))}
          <span className="category-nav-divider" aria-hidden="true" />
          <NavLink to="/auctions">Auctions</NavLink>
          <NavLink to="/artists">Artists</NavLink>
          <NavLink to="/exhibitions">Exhibitions</NavLink>
        </div>
      </nav>
    </header>
  );
}

export function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-grid">
        <div className="footer-brand">
          <Link className="brand brand-light" to="/">ArtConnect</Link>
          <p>A home for original art: discover independent artists, buy with confidence and bid at live auctions.</p>
        </div>
        <div>
          <h2>Discover</h2>
          <ul>
            <li><Link to="/gallery">All art</Link></li>
            <li><Link to="/auctions">Live auctions</Link></li>
            <li><Link to="/artists">Artists</Link></li>
            <li><Link to="/exhibitions">Exhibitions</Link></li>
          </ul>
        </div>
        <div>
          <h2>For artists</h2>
          <ul>
            <li><Link to="/register">Sell your art</Link></li>
            <li><Link to="/studio">Artist studio</Link></li>
            <li><Link to="/exhibitions/new">Curate an exhibition</Link></li>
          </ul>
        </div>
        <div>
          <h2>ArtConnect</h2>
          <ul>
            <li><Link to="/about">About us</Link></li>
            <li><Link to="/contact">Contact</Link></li>
            <li><Link to="/collection">My collection</Link></li>
          </ul>
        </div>
      </div>
      <p className="footer-legal">
        &copy; {new Date().getFullYear()} ArtConnect. All rights reserved. Payments are processed securely by Razorpay.
      </p>
    </footer>
  );
}

export function PageState({ loading, error, children }) {
  if (loading) return <p className="page-state" role="status">Loading...</p>;
  if (error) return <p className="page-state text-danger" role="alert">{error}</p>;
  return children;
}

export function EmptyState({ children }) {
  return <p className="empty-state">{children}</p>;
}

export function Avatar({ src, name = "", size = 40 }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join("");
  return src
    ? <img className="avatar" src={mediaUrl(src)} alt="" width={size} height={size} style={{ width: size, height: size }} />
    : <span className="avatar avatar-fallback" style={{ width: size, height: size, fontSize: size * 0.4 }} aria-hidden="true">{initials || "?"}</span>;
}

export function Stars({ value, count }) {
  const rounded = Math.round(value);
  return (
    <span className="stars" aria-label={`Rated ${value} out of 5${count != null ? ` from ${count} reviews` : ""}`}>
      <span aria-hidden="true">{"★".repeat(rounded)}<span className="stars-empty">{"★".repeat(5 - rounded)}</span></span>
      {count != null && <span className="stars-count">({count})</span>}
    </span>
  );
}

export function Countdown({ endsAt }) {
  const now = useNow(1000);
  return <span className="countdown">{timeLeft(endsAt, now)}</span>;
}

/** A small label for anything other than a plain "buy now" listing. */
export function SaleBadge({ artwork, always = false }) {
  if (artwork.status === "SOLD") return <span className="sale-badge sold">Sold</span>;
  if (artwork.status === "RESERVED") return <span className="sale-badge reserved">Reserved</span>;
  if (artwork.saleType === "AUCTION") {
    return artwork.auctionLive
      ? <span className="sale-badge live">Live auction</span>
      : <span className="sale-badge sold">Auction ended</span>;
  }
  return always ? <span className="sale-badge available">Available</span> : null;
}

export function ArtworkCard({ artwork, children }) {
  const isAuction = artwork.saleType === "AUCTION";
  const details = [artwork.category, artwork.medium].filter(Boolean).join(", ");
  return (
    <article className="art-card">
      <Link to={`/artworks/${artwork.id}`} className="artwork-link">
        <div className="art-card-image">
          <img src={mediaUrl(artwork.imageUrl)} alt={artwork.title} loading="lazy" />
          <div className="art-card-badge"><SaleBadge artwork={artwork} /></div>
        </div>
        <div className="art-card-body">
          <div className="art-card-heading">
            <h3>{artwork.title}</h3>
            {artwork.reviewCount > 0 && <Stars value={artwork.averageRating} />}
          </div>
          <p className="art-card-artist">{artwork.artistName}</p>
          {details && <p className="art-card-meta">{details}{artwork.dimensions && ` · ${artwork.dimensions}`}</p>}
          <div className="art-card-price">
            {isAuction && artwork.status === "AVAILABLE" ? (
              <>
                <span>
                  <small>{artwork.currentBid != null ? "Current bid" : "Starting bid"}</small>
                  {formatPrice(artwork.currentBid ?? artwork.price)}
                </span>
                {artwork.auctionLive && <span className="art-card-ends"><small>Ends in</small><Countdown endsAt={artwork.auctionEndsAt} /></span>}
              </>
            ) : (
              <span className={artwork.status === "AVAILABLE" ? "" : "price-muted"}>
                {artwork.status === "AVAILABLE" ? "" : <small>{artwork.status === "SOLD" ? "Sold" : "Reserved"}</small>}
                {formatPrice(isAuction && artwork.currentBid != null ? artwork.currentBid : artwork.price)}
              </span>
            )}
          </div>
        </div>
      </Link>
      {children && <div className="card-actions">{children}</div>}
    </article>
  );
}

export function ArtworkGrid({ artworks, empty = "No artworks to show yet.", actions }) {
  if (!artworks?.length) return <EmptyState>{empty}</EmptyState>;
  return (
    <div className="gallery-container">
      {artworks.map((artwork) => (
        <ArtworkCard key={artwork.id} artwork={artwork}>{actions?.(artwork)}</ArtworkCard>
      ))}
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination-bar" aria-label="Pages">
      <button type="button" className="btn btn-outline-dark btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button type="button" className="btn btn-outline-dark btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </nav>
  );
}
