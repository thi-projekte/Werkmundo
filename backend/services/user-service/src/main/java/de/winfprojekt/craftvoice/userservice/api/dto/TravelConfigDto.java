package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TravelConfigDto {

    private String modell;
    private boolean modellProvided;
    private BigDecimal pauschale;
    private BigDecimal kmSatz;
    private AddressDto adresse;

    @JsonProperty("modell")
    public String getModell() {
        return modell;
    }

    @JsonProperty("modell")
    public void setModell(String modell) {
        this.modell = modell;
        this.modellProvided = true;
    }

    @JsonIgnore
    public boolean isModellProvided() {
        return modellProvided;
    }

    @JsonProperty("pauschale")
    public BigDecimal getPauschale() {
        return pauschale;
    }

    @JsonProperty("pauschale")
    public void setPauschale(BigDecimal pauschale) {
        this.pauschale = pauschale;
    }

    @JsonProperty("kmSatz")
    public BigDecimal getKmSatz() {
        return kmSatz;
    }

    @JsonProperty("kmSatz")
    public void setKmSatz(BigDecimal kmSatz) {
        this.kmSatz = kmSatz;
    }

    @JsonProperty("adresse")
    public AddressDto getAdresse() {
        return adresse;
    }

    @JsonProperty("adresse")
    public void setAdresse(AddressDto adresse) {
        this.adresse = adresse;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDto {

        private String strasse;
        private boolean strasseProvided;
        private String hausnummer;
        private boolean hausnummerProvided;
        private String plz;
        private boolean plzProvided;
        private String ort;
        private boolean ortProvided;
        private String bundesland;
        private boolean bundeslandProvided;
        private String land;
        private boolean landProvided;

        @JsonProperty("strasse")
        public String getStrasse() {
            return strasse;
        }

        @JsonProperty("strasse")
        public void setStrasse(String strasse) {
            this.strasse = strasse;
            this.strasseProvided = true;
        }

        @JsonIgnore
        public boolean isStrasseProvided() {
            return strasseProvided;
        }

        @JsonProperty("hausnummer")
        public String getHausnummer() {
            return hausnummer;
        }

        @JsonProperty("hausnummer")
        public void setHausnummer(String hausnummer) {
            this.hausnummer = hausnummer;
            this.hausnummerProvided = true;
        }

        @JsonIgnore
        public boolean isHausnummerProvided() {
            return hausnummerProvided;
        }

        @JsonProperty("plz")
        public String getPlz() {
            return plz;
        }

        @JsonProperty("plz")
        public void setPlz(String plz) {
            this.plz = plz;
            this.plzProvided = true;
        }

        @JsonIgnore
        public boolean isPlzProvided() {
            return plzProvided;
        }

        @JsonProperty("ort")
        public String getOrt() {
            return ort;
        }

        @JsonProperty("ort")
        public void setOrt(String ort) {
            this.ort = ort;
            this.ortProvided = true;
        }

        @JsonIgnore
        public boolean isOrtProvided() {
            return ortProvided;
        }

        @JsonProperty("bundesland")
        public String getBundesland() {
            return bundesland;
        }

        @JsonProperty("bundesland")
        public void setBundesland(String bundesland) {
            this.bundesland = bundesland;
            this.bundeslandProvided = true;
        }

        @JsonIgnore
        public boolean isBundeslandProvided() {
            return bundeslandProvided;
        }

        @JsonProperty("land")
        public String getLand() {
            return land;
        }

        @JsonProperty("land")
        public void setLand(String land) {
            this.land = land;
            this.landProvided = true;
        }

        @JsonIgnore
        public boolean isLandProvided() {
            return landProvided;
        }
    }
}
