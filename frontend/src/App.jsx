import { useEffect } from "react";
import { Link, Navigate, Route, Routes, useLocation, useParams } from "react-router-dom";
import { assetUrl } from "./api.js";
import { RequireAuth } from "./auth.jsx";
import { Footer, Header } from "./components.jsx";
import { LoginPage, ProfilePage, RegisterPage } from "./pages/AccountPages.jsx";
import { ArtistProfilePage, ArtistsPage } from "./pages/ArtistPages.jsx";
import ArtworkPage from "./pages/ArtworkPage.jsx";
import BrowsePage from "./pages/BrowsePage.jsx";
import { CartPage, CheckoutPage } from "./pages/CartPages.jsx";
import CollectionPage from "./pages/CollectionPage.jsx";
import { ExhibitionFormPage, ExhibitionPage, ExhibitionsPage } from "./pages/ExhibitionPages.jsx";
import HomePage from "./pages/HomePage.jsx";
import InvoicePage from "./pages/InvoicePage.jsx";
import { AboutPage, ContactPage, NotFoundPage } from "./pages/InfoPages.jsx";
import { ArtworkFormPage, StudioPage } from "./pages/StudioPages.jsx";

function SiteLayout({ children }) {
  const { pathname } = useLocation();

  // Start each new page at the top, as a normal site would
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <>
      <Header />
      <main>{children}</main>
      <Footer />
    </>
  );
}

function AuthenticationPage({ children }) {
  return (
    <div className="auth-page">
      <aside className="auth-art" style={{ backgroundImage: `url("${assetUrl("/images/samples/water-lilies.jpg")}")` }}>
        <Link className="brand brand-light" to="/">ArtConnect</Link>
        <p>Discover original art, support independent artists and bid on pieces you love.</p>
      </aside>
      <div className="auth-content">{children}</div>
    </div>
  );
}

/** Keeps links to the old /artwork/details/:id URL working. */
function LegacyArtworkRedirect() {
  const { id } = useParams();
  return <Navigate to={`/artworks/${id}`} replace />;
}

const site = (page) => <SiteLayout>{page}</SiteLayout>;
const member = (page, options = {}) => site(<RequireAuth {...options}>{page}</RequireAuth>);

export default function App() {
  return (
    <Routes>
      <Route path="/" element={site(<HomePage />)} />
      <Route path="/gallery" element={site(<BrowsePage key="gallery" />)} />
      <Route path="/auctions" element={site(<BrowsePage key="auctions" auctionsOnly />)} />
      <Route path="/artworks/:id" element={site(<ArtworkPage />)} />
      <Route path="/artists" element={site(<ArtistsPage />)} />
      <Route path="/artists/:id" element={site(<ArtistProfilePage />)} />
      <Route path="/exhibitions" element={site(<ExhibitionsPage />)} />
      <Route path="/exhibitions/new" element={member(<ExhibitionFormPage />, { artist: true })} />
      <Route path="/exhibitions/:id" element={site(<ExhibitionPage />)} />
      <Route path="/exhibitions/:id/edit" element={member(<ExhibitionFormPage />, { artist: true })} />
      <Route path="/studio" element={member(<StudioPage />, { artist: true })} />
      <Route path="/studio/upload" element={member(<ArtworkFormPage key="upload" />, { artist: true })} />
      <Route path="/studio/artworks/:id/edit" element={member(<ArtworkFormPage key="edit" />, { artist: true })} />
      <Route path="/collection" element={member(<CollectionPage />)} />
      <Route path="/cart" element={site(<CartPage />)} />
      <Route path="/checkout" element={member(<CheckoutPage />)} />
      <Route path="/checkouts/:id/invoice" element={member(<InvoicePage />)} />
      <Route path="/profile" element={member(<ProfilePage />)} />
      <Route path="/about" element={site(<AboutPage />)} />
      <Route path="/contact" element={site(<ContactPage />)} />
      <Route path="/login" element={<AuthenticationPage><LoginPage /></AuthenticationPage>} />
      <Route path="/register" element={<AuthenticationPage><RegisterPage /></AuthenticationPage>} />

      {/* Old URLs from the previous version of the site */}
      <Route path="/artwork/details/:id" element={<LegacyArtworkRedirect />} />
      <Route path="/artist-dashboard" element={<Navigate to="/studio" replace />} />
      <Route path="/buyer-dashboard" element={<Navigate to="/collection" replace />} />
      <Route path="/upload-artwork" element={<Navigate to="/studio/upload" replace />} />

      <Route path="*" element={site(<NotFoundPage />)} />
    </Routes>
  );
}
