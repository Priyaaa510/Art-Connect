import { Link } from "react-router-dom";
import { mediaUrl, useApi } from "../api.js";
import { ArtworkGrid, Avatar, PageState } from "../components.jsx";
import { ExhibitionCard } from "./ExhibitionPages.jsx";

const PROMISES = [
  { icon: "fa-shield-halved", title: "Secure checkout", text: "Pay safely with UPI, cards, netbanking or wallets." },
  { icon: "fa-gavel", title: "Buy or bid", text: "Buy instantly, or win pieces at live auctions." },
  { icon: "fa-palette", title: "Support artists", text: "Every purchase goes straight to independent sellers." },
];

export default function HomePage() {
  const catalogue = useApi("/api/artworks?sort=top_rated&size=48");
  const auctions = useApi("/api/artworks?liveAuctions=true&sort=ending_soon&size=4");
  const latest = useApi("/api/artworks?status=AVAILABLE&sort=newest&size=8");
  const exhibitions = useApi("/api/exhibitions?status=ONGOING");
  const artists = useApi("/api/artists");
  const categories = useApi("/api/categories");

  const artworks = catalogue.data?.items ?? [];
  // Lead with the best-rated painting that's still for sale
  const featured = artworks.find((artwork) => artwork.status === "AVAILABLE" && artwork.category === "Painting")
    ?? artworks.find((artwork) => artwork.status === "AVAILABLE")
    ?? artworks[0];
  // One cover image per category, taken from its best-rated artwork
  const categoryTiles = (categories.data ?? [])
    .map((category) => ({ category, cover: artworks.find((artwork) => artwork.category === category) }))
    .filter((tile) => tile.cover)
    .slice(0, 8);
  const featuredArtists = (artists.data ?? []).filter((artist) => artist.artworkCount > 0).slice(0, 4);

  return (
    <>
      <section className="home-hero">
        <div className="home-hero-text">
          <p className="eyebrow">Original art, direct from the source</p>
          <h1>Find art you&rsquo;ll love for years to come</h1>
          <p className="home-hero-lead">
            Paintings, photography, sculpture and prints from independent artists and dealers.
            Buy now or bid at auction, with secure checkout.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Link to="/gallery" className="btn btn-primary btn-lg">Shop all art</Link>
            <Link to="/auctions" className="btn btn-outline-dark btn-lg">Live auctions</Link>
          </div>
        </div>
        {featured && (
          <Link to={`/artworks/${featured.id}`} className="home-hero-art">
            <img src={mediaUrl(featured.imageUrl)} alt={featured.title} />
            <span className="home-hero-caption">
              <em>{featured.title}</em> · {featured.artistName}
            </span>
          </Link>
        )}
      </section>

      <section className="promise-strip" aria-label="Why ArtConnect">
        {PROMISES.map((promise) => (
          <div key={promise.title} className="promise">
            <i className={`fas ${promise.icon}`} aria-hidden="true" />
            <div>
              <h2>{promise.title}</h2>
              <p>{promise.text}</p>
            </div>
          </div>
        ))}
      </section>

      {categoryTiles.length > 0 && (
        <HomeSection title="Shop by category" link="/gallery" linkLabel="Browse all">
          <div className="category-tiles">
            {categoryTiles.map(({ category, cover }) => (
              <Link key={category} to={`/gallery?category=${encodeURIComponent(category)}`} className="category-tile">
                <img src={mediaUrl(cover.imageUrl)} alt="" loading="lazy" />
                <span>{category}</span>
              </Link>
            ))}
          </div>
        </HomeSection>
      )}

      <HomeSection title="Live auctions" subtitle="Place your bid before the clock runs out." link="/auctions" linkLabel="All auctions" state={auctions}>
        <ArtworkGrid artworks={auctions.data?.items} empty="No auctions are running right now. Check back soon!" />
      </HomeSection>

      <HomeSection title="New this week" subtitle="Fresh listings from our artists and dealers." link="/gallery" linkLabel="View all" state={latest}>
        <ArtworkGrid artworks={latest.data?.items} empty="No artworks for sale yet. Artists, be the first to upload!" />
      </HomeSection>

      {featuredArtists.length > 0 && (
        <HomeSection title="Featured artists" link="/artists" linkLabel="All artists">
          <div className="featured-artists">
            {featuredArtists.map((artist) => (
              <Link key={artist.id} to={`/artists/${artist.id}`} className="featured-artist">
                <Avatar src={artist.avatarUrl} name={artist.displayName} size={96} />
                <h3>{artist.displayName}</h3>
                <p>{artist.location ?? "ArtConnect artist"}</p>
                <span>{artist.artworkCount} {artist.artworkCount === 1 ? "work" : "works"}</span>
              </Link>
            ))}
          </div>
        </HomeSection>
      )}

      <HomeSection title="Now showing" subtitle="Curated online exhibitions." link="/exhibitions" linkLabel="All exhibitions" state={exhibitions}>
        {exhibitions.data?.length
          ? <div className="exhibition-grid">{exhibitions.data.slice(0, 3).map((e) => <ExhibitionCard key={e.id} exhibition={e} />)}</div>
          : <p className="empty-state">No exhibitions are open right now.</p>}
      </HomeSection>

      <section className="sell-band">
        <div>
          <h2>Are you an artist?</h2>
          <p>Open a free studio, list your work at a fixed price or by auction, and get paid securely online.</p>
        </div>
        <Link to="/register" className="btn btn-light btn-lg">Start selling</Link>
      </section>
    </>
  );
}

function HomeSection({ title, subtitle, link, linkLabel, state, children }) {
  return (
    <section className="home-section">
      <div className="section-header">
        <div>
          <h2 className="section-title">{title}</h2>
          {subtitle && <p className="section-subtitle">{subtitle}</p>}
        </div>
        <Link to={link} className="section-link">{linkLabel} <i className="fas fa-arrow-right" aria-hidden="true" /></Link>
      </div>
      {state ? <PageState loading={state.loading} error={state.error}>{children}</PageState> : children}
    </section>
  );
}
