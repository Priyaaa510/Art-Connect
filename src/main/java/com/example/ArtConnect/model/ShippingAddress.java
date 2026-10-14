package com.example.ArtConnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Where a checkout's artworks are shipped. */
@Embeddable
public class ShippingAddress {
    @Column(name = "ship_name", length = 100)
    private String fullName;

    @Column(name = "ship_line1", length = 200)
    private String line1;

    @Column(name = "ship_line2", length = 200)
    private String line2;

    @Column(name = "ship_city", length = 100)
    private String city;

    @Column(name = "ship_state", length = 100)
    private String state;

    @Column(name = "ship_postal_code", length = 10)
    private String postalCode;

    @Column(name = "ship_country", length = 60)
    private String country;

    protected ShippingAddress() {}

    public ShippingAddress(String fullName, String line1, String line2, String city, String state,
                           String postalCode, String country) {
        this.fullName = fullName;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getFullName() { return fullName; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
}
