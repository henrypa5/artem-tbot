package org.telegram.artem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.telegram.artem.RequestDetails.RequestState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ConnectionBot extends TelegramLongPollingBot {
	
	
	private static Locale l = new Locale("ru", "RU");
	private static ResourceBundle messages = ResourceBundle.getBundle("messages", l, new UTF8Control());
	private Map<String, RequestDetails> activeChats;
	private Map<String, String> answersMap;
	private Writer csvWriter;
	private static final Pattern KEY_EXTRACT = Pattern.compile("(\\.\\d+$)");
	private String token;
	private String adminChatId;
	
	private static final Logger log = LogManager.getLogManager().getLogger(ConnectionBot.class.getName());


	public ConnectionBot(String token, String adminChatId, String csvFileName) throws IOException {
		super();
		
		this.token = token;
		this.adminChatId = adminChatId;
		
		activeChats = new HashMap<String, RequestDetails>(100);
		
		answersMap = new HashMap<String, String>(messages.keySet().size());
		for(String key : messages.keySet()) {
			answersMap.put(messages.getString(key), KEY_EXTRACT.matcher(key).replaceAll(".*"));
		}
		answersMap.put("/start", "hello");
		answersMap.put("/hiddengetbotchats", "print_chats");
		try {
    		String path = Paths.get(
    				getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
    				.resolve(csvFileName).toAbsolutePath().toString();
			File csvFile = new File(path);
			this.csvWriter = new FileWriter(csvFile, true);
			if (!csvFile.exists() || csvFile.length() == 0) {
				writeHeader(csvWriter);
			}
		} catch (URISyntaxException | IOException e) {
			this.csvWriter = new FileWriter(csvFileName);
		}
	}


	private void writeHeader(Writer w) throws IOException {
		String header = RequestDetails.getHeader();
		w.append(header);
		w.flush();
	}


	public void onUpdateReceived(Update update) {
        if (update.hasInlineQuery()) {
            handleIncomingInlineQuery(update.getInlineQuery());
        } 
        if (update.hasCallbackQuery()) {
                handleIncomingCallbackQuery(update.getCallbackQuery());
        } 
        if (update.hasMessage() || update.hasEditedMessage()) {
	        try {
		    	Message m = update.hasEditedMessage() ? update.getEditedMessage() : update.getMessage();

		    	if (m.getFrom().getBot()) {
		    		// Do not react on other Bots
		    		return;
		    	}
		    	
		    	Long chatId = m.getChatId();
		    	RequestDetails rd = getActiveChat(chatId, m.getFrom());
	
		    	SendMessage sm = new SendMessage().setChatId(chatId);
		        
		    	if (m.hasText()) {
		    		if (update.hasEditedMessage()) {
		    			updateRequestDetails(update.getEditedMessage(), sm);
		    		} else {
			    		String answer = answersMap.get(m.getText());
			    		answer = answer == null ? "" : answer;
			    		switch (answer) {
			    		case "hello":
			    			handleHello(sm, m);
			    			break;
			    		case "pushkin":
			    			handlePushkinReply(rd, m, sm, true);
			    			break;
			    		case "whoispushkin":
			    			handlePushkinReply(rd, m, sm, false);
			    			break;
			    		case "district_query_button.*":
			    			handleDistrictReply(rd, m, sm);
			    			break;
			    		case "print_chats":
			    			sm.setText(activeChats.keySet().toString());
			    			break;
			    		default:
			    			handleExtendedState(rd, m, sm, answer);
			    			break;
			    		}
		    		}
		    	} else if (m.hasContact()) {
		    		handlePhoneTimeRequest(rd, m, sm);
		    	}
	    		execute(sm);
	        } catch (TelegramApiException e) {
    	        log.log(Level.SEVERE, e.getMessage(), e);
	        }
	    }
	}


	private void handleExtendedState(RequestDetails rd, Message m, SendMessage sm, String answer) throws TelegramApiException {
		switch (rd.getState()) {
		case ADDRESS_REQUESTED:
			handleAddressRequest(rd, m, sm);
			break;
		case NAME_REQUESTED:
			handleNameRequest(rd, m, sm);
			break;
		case CONTACT_REQUESTED:
			handlePhoneTimeRequest(rd, m, sm);
			break;
		case TIME_REQUESTED:
			handleTimeRequest(rd, m, sm);
			break;
		case FINAL_CHECK_REQUESTED:
			handleFinalCheck(rd, m, sm, answer);
			break;
		default:
			handleHelp(rd, m, sm, answer);
		}
	}

	
	/* Hello and Pushkin */
	private void handleHello(SendMessage sm, Message m) {
		activeChats.remove(getExtendedChatId(m.getChatId(), m.getFrom()));
		addKeyboardImFromPushkin(sm).setText(messages.getString("hello"));
	}
	

	private SendMessage addKeyboardImFromPushkin(SendMessage sm) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow row = new KeyboardRow();
        row.add(messages.getString("pushkin"));
        row.add(messages.getString("whoispushkin"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        sm.setReplyMarkup(keyboardMarkup);
        return sm;
    }
	
	/* District */
	private void handlePushkinReply(RequestDetails rd, Message m, SendMessage sm, boolean fromPushkin) throws TelegramApiException {
		rd.setFromPushkin(fromPushkin, m.getMessageId());
		rd.setState(RequestState.REFERENCE_HANDLED);
		addKeyboardDistrict(sm, "district_query_button").setText(messages.getString("district_query"));	    		
	}
	

	private SendMessage addKeyboardDistrict(SendMessage sm, String key) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow row = new KeyboardRow();
		for (int i = 1; messages.containsKey(key + '.' + i); i++) {
	        row.add(messages.getString(key + '.' + i));
			if (i % 2 == 0) {
				keyboard.add(row);
				row = new KeyboardRow();
			}
		}
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        sm.setReplyMarkup(keyboardMarkup);
        return sm;
	}

	/* Address */
	private void handleDistrictReply(RequestDetails rd, Message m, SendMessage sm) {
		rd.setDistrict(m.getText(), m.getMessageId());
		rd.setState(RequestState.ADDRESS_REQUESTED);
		removeReplyKeyboard(sm).setText(messages.getString("address_query"));
	}
	

	/* Name/Contact */
	private void handleAddressRequest(RequestDetails rd, Message m, SendMessage sm) {
		rd.setAddress(m.getText(), m.getMessageId());
		rd.setState(RequestState.NAME_REQUESTED);
		addKeyboardSendContacts(sm).setText(messages.getString("name_query"));
	}


    /* Phone/Time */
	private void handleNameRequest(RequestDetails rd, Message m, SendMessage sm) {
		rd.setName(m.getText(), m.getMessageId());
		rd.setState(RequestState.CONTACT_REQUESTED);
		addKeyboardSendContacts(sm).setText(messages.getString("phone_time_query"));
	}
	
	private boolean handleContact(RequestDetails rd, Message m, SendMessage sm) {
		if (m.hasContact()) {
			rd.setContactDetails(m.getContact());
			rd.setState(RequestState.TIME_REQUESTED);
			removeReplyKeyboard(sm).setText(messages.getString("time_query"));
			return true;
		}
		return false;
	}
	
    private void handlePhoneTimeRequest(RequestDetails rd, Message m, SendMessage sm) {
		if (!handleContact(rd, m, sm)) {
			rd.setContactDetails(m.getText(), m.getMessageId());
			rd.setState(RequestState.FINAL_CHECK_REQUESTED);
			addKeyboardFinalCheck(sm).setText(
					messages.getString("final_check_text") + rd.toString());
		}
	}

	private SendMessage addKeyboardSendContacts(SendMessage sm) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(messages.getString("send_contact_button"))
        		.setRequestContact(true));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard)
        	.setResizeKeyboard(true)
        	.setSelective(true)
        	.setOneTimeKeyboard(false);
        sm.setReplyMarkup(keyboardMarkup);
		return sm;
	}
    
    /* Time */
	private void handleTimeRequest(RequestDetails rd, Message m, SendMessage sm) {
		rd.setPrefContactTime(m.getText(), m.getMessageId());
		rd.setState(RequestState.FINAL_CHECK_REQUESTED);
		addKeyboardFinalCheck(sm).setText(messages.getString("final_check_text")
				+ "\n" + rd.toString());
	}


	/* Edit */
	private void updateRequestDetails(Message editedMessage, SendMessage sm) {
    	RequestDetails rd = getActiveChat(editedMessage.getChatId(), editedMessage.getFrom());
    	if (rd.updateByMessageId(editedMessage.getMessageId(), editedMessage.getText())) {
    		sm.setText(messages.getString("edit_done_text")
    				+ "\n" + rd.toString());
    		if (rd.isRequestComplete()) {
    			addKeyboardFinalCheck(sm);
    		}
    	} else {
    		handleHello(sm, editedMessage);
    	}
	}


	/* Final check and submit results */
    private void handleFinalCheck(RequestDetails rd, Message m, SendMessage sm, String answer) throws TelegramApiException {
    	switch (answer) {
    	case "final_check_yes_query":
    		handleFinalCheckYes(rd, m, sm);
    		break;
    	case "final_check_no_query":
    		handleFinalCheckEdit(rd, sm);
    		break;
    	default:
    		handleHelp(rd, m, sm, answer);
    		break;
    	}
	}


    /* Yes */
	private void handleFinalCheckYes(RequestDetails rd, Message m, SendMessage sm) throws TelegramApiException {
		rd.setState(RequestState.COMPLETED);
		removeReplyKeyboard(sm).setText(messages.getString("final_check_yes_text")
				+ (rd.isFromPushkin() ? "\n" + messages.getString("pushkin_reminder") : "")
				+ "\n" + messages.getString("help_start"));
		try {
			sendRequestDetails(rd);
		} catch (TelegramApiException e) {
			sm = new SendMessage()
					.setChatId(m.getChatId())
					.setText(messages.getString("falied_to_send_admin_message"));
			execute(removeReplyKeyboard(sm));
		}
		ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
		sm.setReplyMarkup(keyboardMarkup);
	}


	private SendMessage addKeyboardFinalCheck(SendMessage sm) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow row = new KeyboardRow();
        row.add(messages.getString("final_check_yes_query"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        sm.setReplyMarkup(keyboardMarkup);
        return sm;
    }

	/* No */
	private void handleFinalCheckEdit(RequestDetails rd, SendMessage sm) throws TelegramApiException {
		sm.setText(messages.getString("final_check_no_text"));
		rd.setState(RequestState.FINAL_CHECK_REQUESTED);
	}


	/* Send request to the admin chat */
	private void sendRequestDetails(RequestDetails rd) throws TelegramApiException {
    	try {
			if (csvWriter != null) {
				csvWriter.append(rd.toCsvString());
				csvWriter.flush();
			}
		} catch (IOException e) {
    		log.log(Level.SEVERE, messages.getString("cannot_write_to_csv_file"), e);
		}
		String msg = messages.getString("admin_message_text") + " " + rd.getUser().getUserName();
    	SendMessage sm = new SendMessage()
    			.setChatId(adminChatId)
    			.setText(msg + "\n" + rd.toString());
    	execute(sm);
	}


	
	private void handleHelp(RequestDetails rd, Message m, SendMessage sm, String answer) throws TelegramApiException {
		if (rd.getState() != RequestState.NONE) {
			removeReplyKeyboard(sm).setText(messages.getString("help_start"));
		} else {
			addKeyboardImFromPushkin(sm).setText(messages.getString("help"));
		}
	}


	private SendMessage removeReplyKeyboard(SendMessage sm) {
        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
        sm.setReplyMarkup(keyboardMarkup);
		return sm;
	}


	private RequestDetails getActiveChat(Long chatId, User user) {
		RequestDetails rd = activeChats.get(getExtendedChatId(chatId, user));
		if (rd == null) {
			rd = new RequestDetails(chatId, messages);
			rd.setUser(user);
			activeChats.put(getExtendedChatId(chatId, user), rd);
		}
		return rd;
	}


	private String getExtendedChatId(Long chatId, User user) {
		return chatId.toString() + ":" + user.getId();
	}


    private void handleIncomingCallbackQuery(CallbackQuery callbackQuery) {
    	System.out.println(callbackQuery);
	}


	private void handleIncomingInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        System.out.println(query);
	}

	
	public String getBotUsername() {
		return messages.getString("bot_name");
	}

	@Override
	public String getBotToken() {
		return token;
	}

}


