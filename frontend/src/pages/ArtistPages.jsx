import { Link, useParams } from "react-router-dom";
import { useApi } from "../api.js";
import { ArtworkGrid, Avatar, EmptyState, PageState, Stars } from "../components.jsx";
import { formatDate } from "../format.js";
import { ExhibitionCard } from "./ExhibitionPages.jsx";

export function ArtistsPage() {
  const { data: artists, loading, error } = useApi("/api/artists");
  return (
    <section className="container page-section">
      <h1 className="page-title">Artists &amp; dealers</h1>
      <p className="page-intro">Meet the people behind the work.</p>
      <PageState loading={loading} error={error}>
        {artists?.length ? (
          <div className="artist-grid">
            {artists.map((artist) => (
              <Link key={artist.id} to={`/artists/${artist.id}`} className="artist-card">
                <Avatar src={artist.avatarUrl} name={artist.displayName} size={72} />
                <h3>{artist.displayName}</h3>
                {artist.location && <p className="artist-location">{artist.location}</p>}
                {artist.bio && <p className="artist-bio">{artist.bio}</p>}
                <p className="artist-count">{artist.artworkCount} {artist.artworkCount === 1 ? "artwork" : "artworks"}</p>
              </Link>
            ))}
          </div>
        ) : <EmptyState>No artists have joined yet.</EmptyState>}
      </PageState>
    </section>
  );
}

export function ArtistProfilePage() {
  const { id } = useParams();
  const { data, loading, error } = useApi(`/api/artists/${encodeURIComponent(id)}`);

  return (
    <section className="container page-section">
      <PageState loading={loading} error={error}>
        {data && (
          <>
            <header className="profile-header glass-panel">
              <Avatar src={data.artist.avatarUrl} name={data.artist.displayName} size={112} />
              <div>
                <h1>{data.artist.displayName}</h1>
                <p className="profile-meta">
                  @{data.artist.username}
                  {data.artist.location && <> · {data.artist.location}</>}
                  {" "}· Joined {formatDate(data.artist.createdAt)}
                </p>
                {data.reviewCount > 0 && <Stars value={data.averageRating} count={data.reviewCount} />}
                {data.artist.bio && <p className="profile-bio">{data.artist.bio}</p>}
                {data.artist.website && (
                  <a href={data.artist.website} target="_blank" rel="noopener noreferrer">{data.artist.website.replace(/^https?:\/\//, "")}</a>
                )}
              </div>
            </header>

            <h2 className="section-title mt-5">Artworks</h2>
            <ArtworkGrid artworks={data.artworks} empty="This artist hasn't uploaded any artwork yet." />

            {data.exhibitions.length > 0 && (
              <>
                <h2 className="section-title mt-5">Exhibitions</h2>
                <div className="exhibition-grid">
                  {data.exhibitions.map((exhibition) => <ExhibitionCard key={exhibition.id} exhibition={exhibition} />)}
                </div>
              </>
            )}
          </>
        )}
      </PageState>
    </section>
  );
}
