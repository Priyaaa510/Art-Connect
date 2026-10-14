package com.example.ArtConnect.notification;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.example.ArtConnect.dto.MarketDtos.AddressResponse;
import com.example.ArtConnect.dto.MarketDtos.InvoiceResponse;
import com.example.ArtConnect.dto.MarketDtos.NotificationResponse;
import com.example.ArtConnect.dto.MarketDtos.OrderResponse;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.Checkout;
import com.example.ArtConnect.model.Order;
import com.example.ArtConnect.model.OrderSource;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.CheckoutRepository;
import com.example.ArtConnect.repository.NotificationRepository;

/** Builds invoices for paid checkouts and renders them as an email. */
@Service
public class InvoiceService {
    private static final Locale INDIA = new Locale("en", "IN");

    private final CheckoutRepository checkoutRepository;
    private final NotificationRepository notificationRepository;
    private final String currency;
    private final String publicUrl;
    private final ZoneId zone;

    public InvoiceService(CheckoutRepository checkoutRepository, NotificationRepository notificationRepository,
                          @Value("${app.payments.currency:INR}") String currency,
                          @Value("${app.public-url:http://localhost:5173}") String publicUrl,
                          @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.checkoutRepository = checkoutRepository;
        this.notificationRepository = notificationRepository;
        this.currency = currency;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.zone = ZoneId.of(timezone);
    }

    /** The invoice for the buyer or an admin; an artist sees only the items they sold. */
    @Transactional(readOnly = true)
    public InvoiceResponse invoiceFor(User user, Long checkoutId) {
        Checkout checkout = paidCheckout(checkoutId);
        if (checkout.getBuyer().sameAs(user) || user.isAdmin()) {
            return toInvoice(checkout, checkout.getOrders(), false);
        }
        List<Order> sold = checkout.getOrders().stream().filter(o -> o.getArtist().sameAs(user)).toList();
        if (sold.isEmpty()) {
            throw ApiException.forbidden("You can only view invoices for your own orders");
        }
        return toInvoice(checkout, sold, true);
    }

    /** Everything needed to send the invoice, loaded inside one transaction. */
    @Transactional(readOnly = true)
    public InvoiceMessage messageFor(Long checkoutId) {
        Checkout checkout = paidCheckout(checkoutId);
        InvoiceResponse invoice = toInvoice(checkout, checkout.getOrders(), false);
        List<OrderResponse> items = invoice.items();
        String what = items.size() == 1 ? "\"" + items.get(0).artworkTitle() + "\"" : items.size() + " artworks";
        return new InvoiceMessage(checkout.getId(), checkout.getBillingEmail(),
                "Your ArtConnect invoice " + invoice.invoiceNumber() + " for " + what,
                renderHtml(invoice), renderText(invoice));
    }

    /** Rendered invoice email plus its recipient. */
    public record InvoiceMessage(Long checkoutId, String email, String subject, String html, String text) {}

    private Checkout paidCheckout(Long checkoutId) {
        Checkout checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow(() -> ApiException.notFound("Invoice not found"));
        if (checkout.getInvoiceNumber() == null) {
            throw ApiException.conflict("The invoice is available once the payment is complete");
        }
        return checkout;
    }

    private InvoiceResponse toInvoice(Checkout checkout, List<Order> orders, boolean sellerView) {
        List<OrderResponse> items = orders.stream().map(OrderResponse::from).toList();
        BigDecimal total = sellerView
                ? items.stream().map(OrderResponse::price).reduce(BigDecimal.ZERO, BigDecimal::add)
                : checkout.getTotal();
        List<NotificationResponse> notifications = sellerView ? List.of()
                : notificationRepository.findByCheckoutIdOrderByCreatedAtDesc(checkout.getId()).stream()
                        .map(NotificationResponse::from).toList();
        return new InvoiceResponse(checkout.getInvoiceNumber(), checkout.getPaidAt(), checkout.getId(),
                checkout.getBuyer().getId(), checkout.getBuyer().getDisplayName(), checkout.getBillingEmail(),
                AddressResponse.from(checkout.getShippingAddress()), items, total, checkout.getPaymentMethod(),
                checkout.getPaymentId(), currency, sellerView, notifications);
    }

    String invoiceUrl(Long checkoutId) {
        return publicUrl + "/checkouts/" + checkoutId + "/invoice";
    }

    String renderText(InvoiceResponse invoice) {
        String lines = invoice.items().stream()
                .map(item -> "- %s by %s (%s)    %s".formatted(item.artworkTitle(), item.artistName(),
                        sourceLabel(item.source()), money(item.price())))
                .collect(Collectors.joining("\n"));
        return """
                ArtConnect - Invoice %s
                Date: %s

                Billed to: %s (%s)
                Ship to:
                %s

                %s
                Total paid: %s

                Payment: %s, reference %s
                View or print your invoice: %s

                Thank you for supporting independent art!
                """.formatted(invoice.invoiceNumber(), date(invoice.issuedAt()),
                invoice.buyerName(), invoice.billingEmail(), addressLines(invoice.shippingAddress(), "\n"),
                lines, money(invoice.total()),
                methodLabel(invoice.paymentMethod()), invoice.paymentId(), invoiceUrl(invoice.checkoutId()));
    }

    String renderHtml(InvoiceResponse invoice) {
        String rows = invoice.items().stream()
                .map(item -> """
                        <tr style="border-bottom:1px solid #e6e3de"><td><strong>%s</strong><br><span style="color:#6f6f6f">by %s &middot; %s</span></td><td align="center">1</td><td align="right">%s</td></tr>
                        """.formatted(esc(item.artworkTitle()), esc(item.artistName()), esc(sourceLabel(item.source())),
                        esc(money(item.price()))))
                .collect(Collectors.joining());
        String shipTo = invoice.shippingAddress() == null ? "&mdash;"
                : addressLines(invoice.shippingAddress(), "\n").lines().map(InvoiceService::esc)
                        .collect(Collectors.joining("<br>"));
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8"><title>Invoice %1$s</title></head>
                <body style="margin:0;padding:24px;background:#f6f4f0;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e6e3de">
                  <tr><td style="padding:28px 32px;border-bottom:1px solid #e6e3de">
                    <div style="font-family:Georgia,serif;font-size:26px;font-weight:bold">ArtConnect</div>
                    <div style="margin-top:4px;color:#6f6f6f;font-size:13px">Payment received. Thank you for your purchase!</div>
                  </td></tr>
                  <tr><td style="padding:24px 32px">
                    <table role="presentation" width="100%%" style="font-size:14px">
                      <tr>
                        <td valign="top"><div style="color:#6f6f6f;font-size:12px;text-transform:uppercase;letter-spacing:1px">Invoice</div>
                          <div style="font-size:18px;font-weight:bold">%1$s</div>
                          <div style="color:#6f6f6f">%2$s</div></td>
                        <td valign="top" align="right"><div style="color:#6f6f6f;font-size:12px;text-transform:uppercase;letter-spacing:1px">Amount paid</div>
                          <div style="font-size:22px;font-weight:bold">%3$s</div></td>
                      </tr>
                    </table>
                    <table role="presentation" width="100%%" style="margin-top:24px;font-size:14px;line-height:1.5">
                      <tr>
                        <td valign="top" width="50%%"><div style="color:#6f6f6f;font-size:12px;text-transform:uppercase;letter-spacing:1px">Billed to</div>
                          %4$s<br>%5$s</td>
                        <td valign="top" width="50%%"><div style="color:#6f6f6f;font-size:12px;text-transform:uppercase;letter-spacing:1px">Ship to</div>%6$s</td>
                      </tr>
                    </table>
                    <table role="presentation" width="100%%" cellpadding="10" cellspacing="0" style="margin-top:24px;font-size:14px;border-collapse:collapse">
                      <tr style="background:#f6f4f0;text-align:left"><th align="left">Item</th><th align="center">Qty</th><th align="right">Amount</th></tr>
                      %7$s
                      <tr><td colspan="2" align="right"><strong>Total</strong></td><td align="right"><strong>%3$s</strong></td></tr>
                    </table>
                    <p style="margin-top:24px;font-size:14px;line-height:1.6">Paid by <strong>%8$s</strong><br>
                      Payment reference: <code>%9$s</code></p>
                    <p style="margin:28px 0 8px"><a href="%10$s" style="display:inline-block;padding:12px 22px;background:#1a1a1a;color:#ffffff;text-decoration:none">View or print invoice</a></p>
                  </td></tr>
                  <tr><td style="padding:16px 32px;border-top:1px solid #e6e3de;color:#8a8a8a;font-size:12px">
                    This is a computer-generated invoice and does not need a signature.
                  </td></tr>
                </table>
                </body></html>
                """.formatted(esc(invoice.invoiceNumber()), esc(date(invoice.issuedAt())), esc(money(invoice.total())),
                esc(invoice.buyerName()), esc(invoice.billingEmail()), shipTo, rows,
                esc(methodLabel(invoice.paymentMethod())), esc(invoice.paymentId()),
                esc(invoiceUrl(invoice.checkoutId())));
    }

    private static String addressLines(AddressResponse a, String separator) {
        if (a == null) return "";
        StringBuilder sb = new StringBuilder(a.fullName()).append(separator).append(a.line1());
        if (a.line2() != null) sb.append(separator).append(a.line2());
        sb.append(separator).append(a.city()).append(", ").append(a.state()).append(" ").append(a.postalCode());
        if (a.country() != null) sb.append(separator).append(a.country());
        return sb.toString();
    }

    private String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(INDIA);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(amount);
    }

    private String date(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(zone).format(instant);
    }

    static String methodLabel(String method) {
        if (method == null) return "Online payment";
        return switch (method) {
            case "UPI" -> "UPI";
            case "CARD" -> "Debit / credit card";
            case "NETBANKING" -> "Net banking";
            case "WALLET" -> "Wallet";
            default -> "Online payment";
        };
    }

    private static String sourceLabel(OrderSource source) {
        return source == OrderSource.AUCTION ? "Original artwork, won at auction" : "Original artwork";
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
