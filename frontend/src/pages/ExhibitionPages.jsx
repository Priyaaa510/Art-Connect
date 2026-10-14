import { useEffect, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { mediaUrl, request, useApi } from "../api.js";
import { useAuth } from "../auth.jsx";
import { ArtworkGrid, EmptyState, PageState } from "../components.jsx";
import { formatDate, todayIso } from "../format.js";

const STATUS_LABELS = { ONGOING: "Now showing", UPCOMING: "Upcoming", PAST: "Past" };

export function ExhibitionCard({ exhibition }) {
  return (
    <Link to={`/exhibitions/${exhibition.id}`} className="exhibition-card">
      <div className="exhibition-cover">
        {exhibition.coverImageUrl && <img src={mediaUrl(exhibition.coverImageUrl)} alt="" loading="lazy" />}
        <span className={`exhibition-status status-${exhibition.status.toLowerCase()}`}>{STATUS_LABELS[exhibition.status]}</span>
      </div>
      <div className="exhibition-body">
        <h3>{exhibition.title}</h3>
        <p className="exhibition-dates">{formatDate(exhibition.startDate)} – {formatDate(exhibition.endDate)}</p>
        <p className="exhibition-curator">Curated by {exhibition.curatorName} · {exhibition.artworkCount} works</p>
      </div>
    </Link>
  );
}

export function ExhibitionsPage() {
  const { isArtist } = useAuth();
  const [params, setParams] = useSearchParams();
  const status = params.get("status") ?? "ONGOING";
  const { data, loading, error } = useApi(`/api/exhibitions${status === "ALL" ? "" : `?status=${status}`}`);

  return (
    <section className="container page-section">
      <div className="section-header">
        <div>
          <h2 className="mb-1">Exhibitions</h2>
          <p className="lead-muted mb-0">Curated collections from our artists.</p>
        </div>
        {isArtist && <Link to="/exhibitions/new" className="btn btn-primary">Create exhibition</Link>}
      </div>
      <div className="filter-buttons justify-content-start" role="tablist">
        {["ONGOING", "UPCOMING", "PAST", "ALL"].map((value) => (
          <button
            key={value}
            type="button"
            role="tab"
            aria-selected={status === value}
            className={`btn ${status === value ? "btn-primary" : "btn-outline-dark"}`}
            onClick={() => setParams(value === "ONGOING" ? {} : { status: value })}
          >
            {STATUS_LABELS[value] ?? "All"}
          </button>
        ))}
      </div>
      <PageState loading={loading} error={error}>
        {data?.length
          ? <div className="exhibition-grid">{data.map((exhibition) => <ExhibitionCard key={exhibition.id} exhibition={exhibition} />)}</div>
          : <EmptyState>No exhibitions here yet.</EmptyState>}
      </PageState>
    </section>
  );
}

export function ExhibitionPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data, loading, error } = useApi(`/api/exhibitions/${encodeURIComponent(id)}`);
  const [actionError, setActionError] = useState("");
  const exhibition = data?.exhibition;
  const canEdit = exhibition && (user?.id === exhibition.curatorId || user?.role === "ADMIN");

  async function remove() {
    if (!window.confirm("Delete this exhibition? The artworks themselves are kept.")) return;
    try {
      await request(`/api/exhibitions/${exhibition.id}`, { method: "DELETE" });
      navigate("/exhibitions");
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  return (
    <section className="container page-section">
      <PageState loading={loading} error={error}>
        {exhibition && (
          <>
            <header className="glass-panel exhibition-header">
              <span className={`exhibition-status status-${exhibition.status.toLowerCase()}`}>{STATUS_LABELS[exhibition.status]}</span>
              <h1>{exhibition.title}</h1>
              <p className="exhibition-dates">{formatDate(exhibition.startDate)} – {formatDate(exhibition.endDate)}</p>
              <p>Curated by <Link to={`/artists/${exhibition.curatorId}`}>{exhibition.curatorName}</Link></p>
              <p className="exhibition-description">{exhibition.description}</p>
              {canEdit && (
                <div className="d-flex gap-2">
                  <Link to={`/exhibitions/${exhibition.id}/edit`} className="btn btn-outline-dark btn-sm">Edit</Link>
                  <button type="button" className="btn btn-danger btn-sm" onClick={remove}>Delete</button>
                </div>
              )}
              {actionError && <p className="text-danger mt-2" role="alert">{actionError}</p>}
            </header>
            <ArtworkGrid artworks={data.artworks} empty="This exhibition has no artworks." />
          </>
        )}
      </PageState>
    </section>
  );
}

/** Create (no :id) or edit an exhibition. Artists pick from their own artworks. */
export function ExhibitionFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const myArtworks = useApi("/api/me/artworks");
  const existing = useApi(id ? `/api/exhibitions/${encodeURIComponent(id)}` : null);
  const [form, setForm] = useState({ title: "", description: "", startDate: todayIso(), endDate: todayIso() });
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!existing.data) return;
    const { exhibition, artworks } = existing.data;
    setForm({ title: exhibition.title, description: exhibition.description, startDate: exhibition.startDate, endDate: exhibition.endDate });
    setSelected(artworks.map((artwork) => artwork.id));
  }, [existing.data]);

  function toggle(artworkId) {
    setSelected((current) => current.includes(artworkId)
      ? current.filter((value) => value !== artworkId)
      : [...current, artworkId]);
  }

  async function submit(event) {
    event.preventDefault();
    if (!selected.length) {
      setError("Select at least one artwork to show.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const saved = await request(id ? `/api/exhibitions/${id}` : "/api/exhibitions", {
        method: id ? "PUT" : "POST",
        json: { ...form, artworkIds: selected },
      });
      navigate(`/exhibitions/${saved.exhibition.id}`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  const change = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  return (
    <section className="container page-section">
      <h2>{id ? "Edit exhibition" : "Create an exhibition"}</h2>
      <PageState loading={myArtworks.loading || existing.loading} error={myArtworks.error || existing.error}>
        <form className="glass-panel form-panel" onSubmit={submit}>
          <label className="form-label" htmlFor="ex-title">Title</label>
          <input id="ex-title" className="form-control mb-3" maxLength={120} required value={form.title} onChange={change("title")} />
          <label className="form-label" htmlFor="ex-description">Description</label>
          <textarea id="ex-description" className="form-control mb-3" rows="4" maxLength={2000} required value={form.description} onChange={change("description")} />
          <div className="row g-3 mb-3">
            <div className="col-sm-6">
              <label className="form-label" htmlFor="ex-start">Opens</label>
              <input id="ex-start" type="date" className="form-control" required value={form.startDate} onChange={change("startDate")} />
            </div>
            <div className="col-sm-6">
              <label className="form-label" htmlFor="ex-end">Closes</label>
              <input id="ex-end" type="date" className="form-control" required min={form.startDate} value={form.endDate} onChange={change("endDate")} />
            </div>
          </div>

          <fieldset>
            <legend className="form-label">Artworks ({selected.length} selected, shown in the order you pick them)</legend>
            {myArtworks.data?.length ? (
              <div className="picker-grid">
                {myArtworks.data.map((artwork) => {
                  const position = selected.indexOf(artwork.id);
                  return (
                    <label key={artwork.id} className={`picker-item ${position >= 0 ? "selected" : ""}`}>
                      <input type="checkbox" checked={position >= 0} onChange={() => toggle(artwork.id)} />
                      <img src={mediaUrl(artwork.imageUrl)} alt="" />
                      <span>{artwork.title}</span>
                      {position >= 0 && <span className="picker-order">{position + 1}</span>}
                    </label>
                  );
                })}
              </div>
            ) : (
              <p>You need to <Link to="/studio/upload">upload some artwork</Link> first.</p>
            )}
          </fieldset>

          {error && <p className="text-danger mt-3" role="alert">{error}</p>}
          <button className="btn btn-primary mt-3" type="submit" disabled={saving}>{saving ? "Saving..." : id ? "Save changes" : "Create exhibition"}</button>
        </form>
      </PageState>
    </section>
  );
}
