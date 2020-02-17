package org.telegram.artem;

import java.util.Date;
import java.util.ResourceBundle;

import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.User;

public class RequestDetails {

	public enum RequestState {
		NONE,
		REFERENCE_REQUESTED,
		REFERENCE_HANDLED,
		DISTRICT_REQUESTED,
		DISTRICT_HANDLED,
		ADDRESS_REQUESTED,
		ADDRESS_HANDLED,
		NAME_REQUESTED,
		NAME_HANDLED,
		CONTACT_REQUESTED,
		CONTACT_HANDLED,
		TIME_REQUESTED,
		TIME_HANDLED,
		FINAL_CHECK_REQUESTED,
		EDITING,
		COMPLETED
	}
	
	private String district;
	private String address;
	private boolean fromPushkin;
	private String contactDetails;
	private String name;
	private String prefContactTime;
	private User user;
	private long lastUpdateDate;

	private Integer districtMID;
	private Integer addressMID;
	private Integer contactDetailsMID;
	private Integer prefContactTimeMID;
	private Integer nameMID;
	
	private RequestState state = RequestState.NONE;
	
	private Long chatId;
	private ResourceBundle messages;
	
	public RequestDetails(Long chatId, ResourceBundle messages) {
		this.chatId = chatId;
		this.messages = messages;
	}
	
	public boolean updateByMessageId(Integer messageId, String text) {
		if (messageId.equals(districtMID)) {
			setDistrict(text, messageId);
			return true;
		} else if (messageId.equals(addressMID)) {
			setAddress(text, messageId);
			return true;
		} else if (messageId.equals(contactDetailsMID)) {
			setContactDetails(text, messageId);
			return true;
		} else if (messageId.equals(prefContactTimeMID)) {
			setPrefContactTime(text, messageId);
			return true;
		} else if (messageId.equals(nameMID)) {
			setName(text, messageId);
			return true;
		}
		return false;
	}
	
	public String getDistrict() {
		return district;
	}
	
	public void setDistrict(String district, Integer messageId) {
		this.district = district;
		this.districtMID = messageId;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.DISTRICT_HANDLED);
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address, Integer messageId) {
		this.address = address;
		this.addressMID = messageId;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.ADDRESS_HANDLED);
	}
	
	public boolean isFromPushkin() {
		return fromPushkin;
	}
	
	public void setFromPushkin(boolean fromPushkin, Integer messageId) {
		this.fromPushkin = fromPushkin;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.REFERENCE_HANDLED);
	}
	
	public Long getChatId() {
		return chatId;
	}

	public String getContactDetails() {
		return contactDetails;
	}

	public void setContactDetails(Contact contact) {
		StringBuilder contactDetails = new StringBuilder(); 
		contactDetails
			.append(messages.getString("contact_text") + " ")
			.append(formatPhoneNumber(contact.getPhoneNumber()))
			.append(", ")
			.append(contact.getFirstName())
			.append(" ")
			.append(contact.getLastName());
		this.contactDetails = contactDetails.toString();
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.CONTACT_HANDLED);
	}

	private String formatPhoneNumber(String phoneNumber) {
		if (phoneNumber != null) {
			if (phoneNumber.startsWith("+")) {
				phoneNumber = phoneNumber.substring(1);
			}
			return String.valueOf(phoneNumber).replaceFirst("(\\d{3})(\\d{2})(\\d{3})(\\d+)", "+$1 ($2) $3-$4");
		}
		return "";
	}

	public void setContactDetails(String contactDetails, Integer messageId) {
		this.contactDetails = messages.getString("manual_contact_text") + " " + contactDetails;
		this.contactDetailsMID = messageId;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.TIME_HANDLED);
	}

	public String getName() {
		return name;
	}

	public void setName(String name, Integer messageId) {
		this.name = name;
		this.nameMID = messageId;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.NAME_HANDLED);
	}

	public RequestState getState() {
		return state;
	}

	public void setState(RequestState state) {
		this.state = state;
		this.lastUpdateDate = System.currentTimeMillis();
	}

	public String getPrefContactTime() {
		return prefContactTime;
	}

	public void setPrefContactTime(String prefContactTime, Integer messageId) {
		this.prefContactTime = prefContactTime;
		this.prefContactTimeMID = messageId;
		this.lastUpdateDate = System.currentTimeMillis();
//		setState(RequestState.TIME_HANDLED);
	}
	
	public boolean isRequestComplete() {
		return (district != null) && (address != null) && (contactDetails != null);
	}
	
	private String prepareCsvValue(String in) {
		in = in.replaceAll("\"", "\"\"");
		return "\"" + in + "\"";
	}
	
	public String toCsvString() {
		return prepareCsvValue(user.getId().toString()) + ","
				+ prepareCsvValue(new Date(lastUpdateDate).toString()) + ","
				+ prepareCsvValue((fromPushkin ? messages.getString("pushkin") : "")) + "," 
				+ prepareCsvValue(getDistrict()) + "," 
				+ prepareCsvValue(getAddress()) + "," 
				+ prepareCsvValue((getName() != null && getName().length() > 0 ? getName() : "")) + ","
				+ prepareCsvValue(getContactDetails()
					+ (getPrefContactTime() != null ? "\n" + messages.getString("time_text") + " " + getPrefContactTime() : ""))
				+ "\n";
	}
	
	public String toString() {
		return (fromPushkin ? messages.getString("pushkin") + "\n" : "") 
				+ messages.getString("district_text") + " " + getDistrict() 
				+ "\n" + messages.getString("address_text") + " " + getAddress() 
				+ (getName() != null && getName().length() > 0 ? 
						"\n" + messages.getString("name_text") + " " + getName() : "") 
				+ "\n" + getContactDetails() 
				+ (getPrefContactTime() != null && getPrefContactTime().length() > 0 
					? "\n" + messages.getString("time_text") + " " + getPrefContactTime() : "");
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		this.lastUpdateDate = System.currentTimeMillis();
	}

	public long getLastUpdateDate() {
		return lastUpdateDate;
	}

	public static String getHeader() {
		return "\uFEFF\"User ID\",\"Дата обновления\",\"Источник\",\"Район\",\"Адрес\",\"Имя\",\"Контакты и время\"\n";
	}
}
