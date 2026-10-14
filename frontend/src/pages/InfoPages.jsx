import { Link } from "react-router-dom";

/** Where contact-form messages are delivered (via FormSubmit). Set VITE_CONTACT_EMAIL in frontend/.env.local. */
const CONTACT_EMAIL = import.meta.env.VITE_CONTACT_EMAIL?.trim();

export function AboutPage() {
  return (
    <section className="info-page">
      <h1>About ArtConnect</h1>
      <p>ArtConnect is a digital space where creativity meets technology. It is designed to help artists showcase their artwork online and share their visual stories with a wider audience beyond physical boundaries.</p>
      <p>Artists build a public profile, upload their work with descriptions and pricing, sell at a fixed price or through timed auctions, and curate online exhibitions. Collectors browse by category, buy or bid on pieces, and leave reviews that help great work get discovered.</p>
      <p>Many talented artists struggle to find platforms where their work can be presented professionally and discovered easily. ArtConnect was created to bridge that gap with a simple, elegant, and accessible online gallery experience for both creators and viewers.</p>
      <p>Built with React, Spring Boot, PostgreSQL, and Cloudinary, ArtConnect combines cloud image storage with a full-stack architecture to create a responsive and scalable art community.</p>
      <p>ArtConnect is more than a gallery; it is a step toward building a connected digital community for emerging artists.</p>
    </section>
  );
}

export function ContactPage() {
  return (
    <section className="info-page">
      <h1>Contact us</h1>
      <p>Questions about an order, selling your work, or anything else? Send us a message and our team will get back to you as soon as possible.</p>
      <p>For questions about a specific artwork, you can also reach the seller through their profile on the <Link to="/artists">Artists</Link> page.</p>
      {CONTACT_EMAIL ? (
        <form action={`https://formsubmit.co/${encodeURIComponent(CONTACT_EMAIL)}`} method="POST">
          <input type="text" name="name" placeholder="Your name" aria-label="Your name" required />
          <input type="email" name="email" placeholder="Your email" aria-label="Your email" required />
          <textarea name="message" rows="5" placeholder="How can we help?" aria-label="Your message" required />
          <input type="text" name="_honey" className="honeypot" tabIndex="-1" autoComplete="off" />
          <input type="hidden" name="_captcha" value="false" />
          <button type="submit">Send message</button>
        </form>
      ) : (
        <p className="empty-state">The contact form isn't available right now. Please try again later.</p>
      )}
    </section>
  );
}

export function NotFoundPage() {
  return (
    <section className="container page-section text-center">
      <h1>Page not found</h1>
      <Link to="/" className="btn btn-primary">Return home</Link>
    </section>
  );
}
