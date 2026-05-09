package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Locale;
import java.util.Map;

/**
 * Represents a student or faculty issued guest pass.
 * Pattern: Immutable POJO used by pass generation and cancellation UI flows.
 * Known issue: QR content is represented by passCode string in v1.
 */
public class GuestPass {
	private final String id;
	private final String sponsorUid;
	private final String sponsorRole;
	private final String sponsorName;
	private final String sponsorEmail;
	private final String sponsorStudentId;
	private final String guestName;
	private final String guestIdNumber;
	private final String guestPhone;
	private final boolean hasVehicle;
	private final String vehiclePlate;
	private final String guestType;
	private final String passCode;
	private final String entryRequestId;
	private final String gateLabel;
	private final String status;
	private final Timestamp expiresAt;
	private final Timestamp admittedAt;
	private final String admittedByUid;
	private final String admissionMethod;
	private final Timestamp createdAt;

	/**
	 * Creates an immutable guest pass record.
	 */
	public GuestPass(
			@NonNull String id,
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String sponsorStudentId,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			@NonNull String guestPhone,
			boolean hasVehicle,
			@NonNull String vehiclePlate,
			@NonNull String guestType,
			@NonNull String passCode,
			@NonNull String entryRequestId,
			@NonNull String gateLabel,
			@NonNull String status,
			@Nullable Timestamp expiresAt,
			@Nullable Timestamp admittedAt,
			@NonNull String admittedByUid,
			@NonNull String admissionMethod,
			@Nullable Timestamp createdAt
	) {
		this.id = id;
		this.sponsorUid = sponsorUid;
		this.sponsorRole = sponsorRole;
		this.sponsorName = sponsorName;
		this.sponsorEmail = sponsorEmail;
		this.sponsorStudentId = sponsorStudentId.trim();
		this.guestName = guestName;
		String normalizedGuestId = GuestIdentityPolicy.normalizeCnic(guestIdNumber);
		this.guestIdNumber = normalizedGuestId == null ? guestIdNumber : normalizedGuestId;
		String normalizedPhone = GuestIdentityPolicy.normalizePhone(guestPhone);
		this.guestPhone = normalizedPhone == null ? guestPhone.trim() : normalizedPhone;
		this.hasVehicle = hasVehicle;
		String normalizedPlate = GuestIdentityPolicy.normalizeVehiclePlate(vehiclePlate);
		this.vehiclePlate = normalizedPlate == null
				? vehiclePlate.trim().toUpperCase(Locale.getDefault())
				: normalizedPlate;
		this.guestType = guestType.trim().isEmpty() ? GuestIdentityPolicy.guestTypeFor(hasVehicle) : guestType;
		this.passCode = passCode;
		this.entryRequestId = entryRequestId;
		this.gateLabel = GatePolicy.normalizeStoredValue(gateLabel);
		this.status = status;
		this.expiresAt = expiresAt;
		this.admittedAt = admittedAt;
		this.admittedByUid = admittedByUid;
		this.admissionMethod = admissionMethod;
		this.createdAt = createdAt;
	}

	public GuestPass(
			@NonNull String id,
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			boolean hasVehicle,
			@NonNull String vehiclePlate,
			@NonNull String guestType,
			@NonNull String passCode,
			@NonNull String entryRequestId,
			@NonNull String gateLabel,
			@NonNull String status,
			@Nullable Timestamp expiresAt,
			@Nullable Timestamp admittedAt,
			@NonNull String admittedByUid,
			@NonNull String admissionMethod,
			@Nullable Timestamp createdAt
	) {
		this(id, sponsorUid, sponsorRole, sponsorName, sponsorEmail, "", guestName, guestIdNumber, "",
				hasVehicle, vehiclePlate, guestType, passCode, entryRequestId, gateLabel, status,
				expiresAt, admittedAt, admittedByUid, admissionMethod, createdAt);
	}

	public GuestPass(
			@NonNull String id,
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			@NonNull String guestPhone,
			boolean hasVehicle,
			@NonNull String vehiclePlate,
			@NonNull String guestType,
			@NonNull String passCode,
			@NonNull String entryRequestId,
			@NonNull String gateLabel,
			@NonNull String status,
			@Nullable Timestamp expiresAt,
			@Nullable Timestamp admittedAt,
			@NonNull String admittedByUid,
			@NonNull String admissionMethod,
			@Nullable Timestamp createdAt
	) {
		this(id, sponsorUid, sponsorRole, sponsorName, sponsorEmail, "", guestName, guestIdNumber, guestPhone,
				hasVehicle, vehiclePlate, guestType, passCode, entryRequestId, gateLabel, status,
				expiresAt, admittedAt, admittedByUid, admissionMethod, createdAt);
	}

	/**
	 * Maps Firestore data into a {@link GuestPass}.
	 *
	 * @param id Firestore document id.
	 * @param map Firestore field map.
	 * @return parsed pass object, or a safe empty fallback when map is missing.
	 */
	@NonNull
	public static GuestPass fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new GuestPass(
					id,
					"",
					"",
					"",
					"",
					"",
					"",
					"",
					"",
					false,
					"",
					"non_vehicle",
					"",
					"",
					GatePolicy.STORED_VALUE,
					"",
					null,
					null,
					"",
					"",
					null
			);
		}
		return new GuestPass(
				id,
				asString(map.get("sponsorUid")),
				asString(map.get("sponsorRole")),
				asString(map.get("sponsorName")),
				asString(map.get("sponsorEmail")),
				asStringOr(map.get("sponsorStudentId"), asString(map.get("studentId"))),
				asString(map.get("guestName")),
				asString(map.get("guestIdNumber")),
				asString(map.get("guestPhone")),
				asBoolean(map.get("hasVehicle")),
				asString(map.get("vehiclePlate")),
				asString(map.get("guestType")),
				asString(map.get("passCode")),
				asString(map.get("entryRequestId")),
				GatePolicy.normalizeStoredValue(asString(map.get("gateLabel"))),
				asString(map.get("status")),
				asTimestamp(map.get("expiresAt")),
				asTimestamp(map.get("admittedAt")),
				asString(map.get("admittedByUid")),
				asString(map.get("admissionMethod")),
				asTimestamp(map.get("createdAt"))
		);
	}

	/**
	 * @return display name of sponsoring user.
	 */
	@NonNull
	public String getSponsorName() {
		return sponsorName;
	}

	/**
	 * @return sponsor email used for contact/reference.
	 */
	@NonNull
	public String getSponsorEmail() {
		return sponsorEmail;
	}

	/**
	 * @return student id of the sponsoring student, when applicable.
	 */
	@NonNull
	public String getSponsorStudentId() {
		return sponsorStudentId;
	}

	/**
	 * @return Firestore document id.
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * @return uid of the pass sponsor.
	 */
	@NonNull
	public String getSponsorUid() {
		return sponsorUid;
	}

	/**
	 * @return role of the sponsor (student/faculty).
	 */
	@NonNull
	public String getSponsorRole() {
		return sponsorRole;
	}

	/**
	 * @return guest full name.
	 */
	@NonNull
	public String getGuestName() {
		return guestName;
	}

	/**
	 * @return guest identifier (ID/CNIC/passport value).
	 */
	@NonNull
	public String getGuestIdNumber() {
		return guestIdNumber;
	}

	/**
	 * @return guest phone number captured at pass creation.
	 */
	@NonNull
	public String getGuestPhone() {
		return guestPhone;
	}

	public boolean hasVehicle() {
		return hasVehicle;
	}

	@NonNull
	public String getVehiclePlate() {
		return vehiclePlate;
	}

	@NonNull
	public String getGuestType() {
		return guestType;
	}

	/**
	 * @return short pass code used in QR/manual verification.
	 */
	@NonNull
	public String getPassCode() {
		return passCode;
	}

	/**
	 * @return linked entry request document id.
	 */
	@NonNull
	public String getEntryRequestId() {
		return entryRequestId;
	}

	/**
	 * @return gate label associated with this pass.
	 */
	@NonNull
	public String getGateLabel() {
		return gateLabel;
	}

	/**
	 * @return lifecycle status of the pass.
	 */
	@NonNull
	public String getStatus() {
		return status;
	}

	/**
	 * @return pass expiry time, if available.
	 */
	@Nullable
	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	/**
	 * @return timestamp when pass was admitted/consumed.
	 */
	@Nullable
	public Timestamp getAdmittedAt() {
		return admittedAt;
	}

	/**
	 * @return uid of guard who admitted the pass.
	 */
	@NonNull
	public String getAdmittedByUid() {
		return admittedByUid;
	}

	/**
	 * @return admission method (QR_SCAN or PASS_CODE).
	 */
	@NonNull
	public String getAdmissionMethod() {
		return admissionMethod;
	}

	/**
	 * @return creation timestamp from Firestore.
	 */
	@Nullable
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	@NonNull
	private static String asStringOr(@Nullable Object value, @NonNull String fallback) {
		String parsed = asString(value);
		return parsed.trim().isEmpty() ? fallback : parsed;
	}

	private static boolean asBoolean(@Nullable Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		if (value instanceof String) {
			String normalized = ((String) value).trim().toLowerCase(Locale.getDefault());
			return "true".equals(normalized) || "1".equals(normalized);
		}
		return false;
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}
}
