package pl.riftplots.domain.plot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Plot {
    private final String id;
    private String name;
    private String description;
    private PlotCategory category;
    private PlotStatus status;
    private PlotBounds bounds;
    private String worldGuardRegionId;

    private BigDecimal price;
    private BigDecimal baseTax;
    private TaxMode taxMode;
    private double taxMultiplier;
    private boolean taxEnabled;
    private int billingPeriodDays;
    private int gracePeriodHours;

    private UUID ownerUuid;
    private String ownerName;
    private final Map<UUID, PlotMember> members = new ConcurrentHashMap<>();

    private Instant purchaseDate;
    private Instant nextTaxPayment;
    private Instant gracePeriodExpiry;
    private int advancePeriodsPaid;

    private String schematicFile;
    private boolean autoResetOnExpire;
    private double refundPercentOnAbandon;

    private String propertyType = "RESIDENTIAL";
    private String districtId;
    private boolean rentalAvailable;
    private BigDecimal rentalPrice = BigDecimal.ZERO;
    private String rentalPeriodUnit = "DAY";
    private BigDecimal rentalDeposit = BigDecimal.ZERO;
    private String activeRentalId;

    private String homeWorld;
    private Double homeX;
    private Double homeY;
    private Double homeZ;
    private Float homeYaw;
    private Float homePitch;

    private BigDecimal resellPrice = BigDecimal.ZERO;
    private boolean resellAvailable;
    private boolean auctionActive;
    // Old records default to explicit values so upgrading does not reprice existing contracts.
    private final Set<PropertySetting> settingOverrides = EnumSet.allOf(PropertySetting.class);

    public Set<PropertySetting> getSettingOverrides() {
        return Collections.unmodifiableSet(settingOverrides);
    }

    public void setSettingOverrides(Set<PropertySetting> settings) {
        settingOverrides.clear();
        settingOverrides.addAll(settings);
    }

    public boolean overrides(PropertySetting setting) {
        return settingOverrides.contains(setting);
    }

    public void applyTypeDefaults(pl.riftplots.domain.type.PropertyType type) {
        price = type.getDefaultPrice();
        baseTax = type.getDefaultBaseTax();
        taxMode = type.getDefaultTaxMode();
        taxMultiplier = type.getDefaultTaxMultiplier();
        billingPeriodDays = type.getDefaultBillingPeriodDays();
        refundPercentOnAbandon = type.getRefundPercent();
        settingOverrides.clear();
        touch();
    }

    private final Instant createdAt;
    private Instant updatedAt;

    public Plot(String id,
                String name,
                String description,
                PlotCategory category,
                PlotStatus status,
                PlotBounds bounds,
                String worldGuardRegionId,
                BigDecimal price,
                BigDecimal baseTax,
                TaxMode taxMode,
                double taxMultiplier,
                boolean taxEnabled,
                int billingPeriodDays,
                int gracePeriodHours,
                UUID ownerUuid,
                String ownerName,
                Collection<PlotMember> membersList,
                Instant purchaseDate,
                Instant nextTaxPayment,
                Instant gracePeriodExpiry,
                int advancePeriodsPaid,
                String schematicFile,
                boolean autoResetOnExpire,
                double refundPercentOnAbandon,
                Instant createdAt,
                Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null").toLowerCase();
        this.name = name != null ? name : id;
        this.description = description != null ? description : "";
        this.category = category != null ? category : PlotCategory.RESIDENTIAL;
        this.status = status != null ? status : PlotStatus.AVAILABLE;
        this.bounds = Objects.requireNonNull(bounds, "bounds cannot be null");
        this.worldGuardRegionId = worldGuardRegionId != null ? worldGuardRegionId : ("riftplot_" + this.id);
        this.price = price != null ? price.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        this.baseTax = baseTax != null ? baseTax.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        this.taxMode = taxMode != null ? taxMode : TaxMode.FIXED;
        this.taxMultiplier = taxMultiplier > 0 ? taxMultiplier : 1.0;
        this.taxEnabled = taxEnabled;
        this.billingPeriodDays = billingPeriodDays > 0 ? billingPeriodDays : 7;
        this.gracePeriodHours = gracePeriodHours >= 0 ? gracePeriodHours : 6;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        if (membersList != null) {
            for (PlotMember member : membersList) {
                this.members.put(member.getPlayerUuid(), member);
            }
        }
        this.purchaseDate = purchaseDate;
        this.nextTaxPayment = nextTaxPayment;
        this.gracePeriodExpiry = gracePeriodExpiry;
        this.advancePeriodsPaid = Math.max(0, advancePeriodsPaid);
        this.schematicFile = schematicFile;
        this.autoResetOnExpire = autoResetOnExpire;
        this.refundPercentOnAbandon = refundPercentOnAbandon;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public PlotCategory getCategory() {
        return category;
    }

    public void setCategory(PlotCategory category) {
        this.category = category;
        touch();
    }

    public PlotStatus getStatus() {
        return status;
    }

    public void setStatus(PlotStatus status) {
        this.status = status;
        touch();
    }

    public PlotBounds getBounds() {
        return bounds;
    }

    public void setBounds(PlotBounds bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds cannot be null");
        touch();
    }

    public String getWorldGuardRegionId() {
        return worldGuardRegionId;
    }

    public void setWorldGuardRegionId(String worldGuardRegionId) {
        this.worldGuardRegionId = worldGuardRegionId;
        touch();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        settingOverrides.add(PropertySetting.PRICE);
        this.price = price != null ? price.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        touch();
    }

    public BigDecimal getBaseTax() {
        return baseTax;
    }

    public void setBaseTax(BigDecimal baseTax) {
        settingOverrides.add(PropertySetting.BASE_TAX);
        this.baseTax = baseTax != null ? baseTax.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        touch();
    }

    public TaxMode getTaxMode() {
        return taxMode;
    }

    public void setTaxMode(TaxMode taxMode) {
        settingOverrides.add(PropertySetting.TAX_MODE);
        this.taxMode = taxMode != null ? taxMode : TaxMode.FIXED;
        touch();
    }

    public double getTaxMultiplier() {
        return taxMultiplier;
    }

    public void setTaxMultiplier(double taxMultiplier) {
        settingOverrides.add(PropertySetting.TAX_MULTIPLIER);
        this.taxMultiplier = taxMultiplier > 0 ? taxMultiplier : 1.0;
        touch();
    }

    public boolean isTaxEnabled() {
        return taxEnabled;
    }

    public void setTaxEnabled(boolean taxEnabled) {
        this.taxEnabled = taxEnabled;
        touch();
    }

    public int getBillingPeriodDays() {
        return billingPeriodDays;
    }

    public void setBillingPeriodDays(int billingPeriodDays) {
        settingOverrides.add(PropertySetting.BILLING_PERIOD);
        this.billingPeriodDays = billingPeriodDays > 0 ? billingPeriodDays : 7;
        touch();
    }

    public int getGracePeriodHours() {
        return gracePeriodHours;
    }

    public void setGracePeriodHours(int gracePeriodHours) {
        this.gracePeriodHours = gracePeriodHours >= 0 ? gracePeriodHours : 6;
        touch();
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        touch();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        touch();
    }

    public Map<UUID, PlotMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public PlotMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public void addMember(PlotMember member) {
        if (member != null) {
            members.put(member.getPlayerUuid(), member);
            touch();
        }
    }

    public PlotMember removeMember(UUID uuid) {
        PlotMember removed = members.remove(uuid);
        if (removed != null) {
            touch();
        }
        return removed;
    }

    public void clearMembers() {
        members.clear();
        touch();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean hasPermission(UUID uuid, PlotPermission permission) {
        if (isOwner(uuid)) {
            return true;
        }
        PlotMember member = members.get(uuid);
        return member != null && member.hasPermission(permission);
    }

    public Instant getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Instant purchaseDate) {
        this.purchaseDate = purchaseDate;
        touch();
    }

    public Instant getNextTaxPayment() {
        return nextTaxPayment;
    }

    public void setNextTaxPayment(Instant nextTaxPayment) {
        this.nextTaxPayment = nextTaxPayment;
        touch();
    }

    public Instant getGracePeriodExpiry() {
        return gracePeriodExpiry;
    }

    public void setGracePeriodExpiry(Instant gracePeriodExpiry) {
        this.gracePeriodExpiry = gracePeriodExpiry;
        touch();
    }

    public int getAdvancePeriodsPaid() {
        if (nextTaxPayment == null) return 0;
        long remainingSeconds = java.time.Duration.between(Instant.now(), nextTaxPayment).getSeconds();
        long periodSeconds = java.time.Duration.ofDays(billingPeriodDays).getSeconds();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (remainingSeconds - 1) / periodSeconds));
    }

    public void setAdvancePeriodsPaid(int advancePeriodsPaid) {
        this.advancePeriodsPaid = Math.max(0, advancePeriodsPaid);
        touch();
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public void setSchematicFile(String schematicFile) {
        this.schematicFile = schematicFile;
        touch();
    }

    public boolean isAutoResetOnExpire() {
        return autoResetOnExpire;
    }

    public void setAutoResetOnExpire(boolean autoResetOnExpire) {
        this.autoResetOnExpire = autoResetOnExpire;
        touch();
    }

    public double getRefundPercentOnAbandon() {
        return refundPercentOnAbandon;
    }

    public void setRefundPercentOnAbandon(double refundPercentOnAbandon) {
        settingOverrides.add(PropertySetting.REFUND_PERCENT);
        this.refundPercentOnAbandon = refundPercentOnAbandon;
        touch();
    }

    public String getPropertyType() {
        return propertyType != null ? propertyType : "RESIDENTIAL";
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType != null ? propertyType.toUpperCase() : "RESIDENTIAL";
        touch();
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId != null ? districtId.toLowerCase() : null;
        touch();
    }

    public boolean isRentalAvailable() {
        return rentalAvailable;
    }

    public void setRentalAvailable(boolean rentalAvailable) {
        this.rentalAvailable = rentalAvailable;
        touch();
    }

    public BigDecimal getRentalPrice() {
        return rentalPrice != null ? rentalPrice : BigDecimal.ZERO;
    }

    public void setRentalPrice(BigDecimal rentalPrice) {
        this.rentalPrice = rentalPrice != null ? rentalPrice.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        touch();
    }

    public String getRentalPeriodUnit() {
        return rentalPeriodUnit != null ? rentalPeriodUnit : "DAY";
    }

    public void setRentalPeriodUnit(String rentalPeriodUnit) {
        this.rentalPeriodUnit = rentalPeriodUnit != null ? rentalPeriodUnit.toUpperCase() : "DAY";
        touch();
    }

    public BigDecimal getRentalDeposit() {
        return rentalDeposit != null ? rentalDeposit : BigDecimal.ZERO;
    }

    public void setRentalDeposit(BigDecimal rentalDeposit) {
        this.rentalDeposit = rentalDeposit != null ? rentalDeposit.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        touch();
    }

    public String getActiveRentalId() {
        return activeRentalId;
    }

    public void setActiveRentalId(String activeRentalId) {
        this.activeRentalId = activeRentalId;
        touch();
    }

    public boolean isRented() {
        return activeRentalId != null && !activeRentalId.isBlank();
    }

    public String getHomeWorld() {
        return homeWorld;
    }

    public Double getHomeX() {
        return homeX;
    }

    public Double getHomeY() {
        return homeY;
    }

    public Double getHomeZ() {
        return homeZ;
    }

    public Float getHomeYaw() {
        return homeYaw;
    }

    public Float getHomePitch() {
        return homePitch;
    }

    public void setHomeLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.homeWorld = world;
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
        touch();
    }

    public void clearHomeLocation() {
        this.homeWorld = null;
        this.homeX = null;
        this.homeY = null;
        this.homeZ = null;
        this.homeYaw = null;
        this.homePitch = null;
        touch();
    }

    public boolean hasHomeLocation() {
        return homeWorld != null && homeX != null && homeY != null && homeZ != null;
    }

    public BigDecimal getResellPrice() {
        return resellPrice != null ? resellPrice : BigDecimal.ZERO;
    }

    public void setResellPrice(BigDecimal resellPrice) {
        this.resellPrice = resellPrice != null ? resellPrice.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        touch();
    }

    public boolean isResellAvailable() {
        return resellAvailable;
    }

    public void setResellAvailable(boolean resellAvailable) {
        this.resellAvailable = resellAvailable;
        touch();
    }

    public boolean isAuctionActive() {
        return auctionActive;
    }

    public boolean isAvailableForPurchase() {
        return status == PlotStatus.AVAILABLE && ownerUuid == null
                && !rentalAvailable && !isRented() && !auctionActive;
    }

    public void clearOwnership() {
        ownerUuid = null;
        ownerName = null;
        clearMembers();
        purchaseDate = null;
        nextTaxPayment = null;
        gracePeriodExpiry = null;
        advancePeriodsPaid = 0;
        activeRentalId = null;
        auctionActive = false;
        resellAvailable = false;
        resellPrice = BigDecimal.ZERO;
        clearHomeLocation();
        touch();
    }

    public void setAuctionActive(boolean auctionActive) {
        this.auctionActive = auctionActive;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Plot copy() {
        Plot copy = new Plot(id, name, description, category, status, bounds, worldGuardRegionId, price, baseTax,
                taxMode, taxMultiplier, taxEnabled, billingPeriodDays, gracePeriodHours, ownerUuid, ownerName,
                List.of(), purchaseDate, nextTaxPayment, gracePeriodExpiry, advancePeriodsPaid, schematicFile,
                autoResetOnExpire, refundPercentOnAbandon, createdAt, updatedAt);
        copy.restore(this);
        return copy;
    }

    public void restore(Plot state) {
        if (this == state) return;
        if (!id.equals(state.id)) throw new IllegalArgumentException("Different plot ID");
        name = state.name;
        description = state.description;
        category = state.category;
        status = state.status;
        bounds = state.bounds;
        worldGuardRegionId = state.worldGuardRegionId;
        price = state.price;
        baseTax = state.baseTax;
        taxMode = state.taxMode;
        taxMultiplier = state.taxMultiplier;
        taxEnabled = state.taxEnabled;
        billingPeriodDays = state.billingPeriodDays;
        gracePeriodHours = state.gracePeriodHours;
        ownerUuid = state.ownerUuid;
        ownerName = state.ownerName;
        members.clear();
        for (PlotMember member : state.members.values())
            members.put(member.getPlayerUuid(), new PlotMember(
                    member.getPlayerUuid(), member.getPlayerName(), member.getRole(), member.getPermissions(), member.getAddedAt()));
        purchaseDate = state.purchaseDate;
        nextTaxPayment = state.nextTaxPayment;
        gracePeriodExpiry = state.gracePeriodExpiry;
        advancePeriodsPaid = state.advancePeriodsPaid;
        schematicFile = state.schematicFile;
        autoResetOnExpire = state.autoResetOnExpire;
        refundPercentOnAbandon = state.refundPercentOnAbandon;
        propertyType = state.propertyType;
        districtId = state.districtId;
        rentalAvailable = state.rentalAvailable;
        rentalPrice = state.rentalPrice;
        rentalPeriodUnit = state.rentalPeriodUnit;
        rentalDeposit = state.rentalDeposit;
        activeRentalId = state.activeRentalId;
        homeWorld = state.homeWorld;
        homeX = state.homeX;
        homeY = state.homeY;
        homeZ = state.homeZ;
        homeYaw = state.homeYaw;
        homePitch = state.homePitch;
        resellPrice = state.resellPrice;
        resellAvailable = state.resellAvailable;
        auctionActive = state.auctionActive;
        updatedAt = state.updatedAt;
        settingOverrides.clear();
        settingOverrides.addAll(state.settingOverrides);
    }
}
