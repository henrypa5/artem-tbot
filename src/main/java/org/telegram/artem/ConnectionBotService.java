package org.telegram.artem;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.log4j.PropertyConfigurator;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;

public class ConnectionBotService implements Daemon {

	private static final Logger log = LogManager.getLogManager().getLogger(ConnectionBotService.class.getName());
	private static Locale l = new Locale("ru", "RU");
	private static ResourceBundle messages = ResourceBundle.getBundle("messages", l, new UTF8Control());
	
	public static void main(String[] args) throws Exception {
		ConnectionBotService cbs = new ConnectionBotService();
		cbs.init(null);
		cbs.start();
	}


	private String token;
	private BotSession session;
	private String adminChatId;


	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		PropertyConfigurator.configure("log4j.properties");
		token = System.getenv("CONNECT_ARTEM_BOT_TOKEN");
		if (token == null || token.equals("")) {
			throw new DaemonInitException(messages.getString("no_token_found"));
		}

		adminChatId = System.getenv("CONNECT_ARTEM_BOT_ADMIN_CHAT_ID");
		if (adminChatId == null || adminChatId.equals("")) {
			throw new DaemonInitException(messages.getString("admin_chat_not_defined"));
		}

        ApiContextInitializer.init();
	}


	@Override
	public void start() throws Exception {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
        	try {
        		session = telegramBotsApi.registerBot(new ConnectionBot(token, adminChatId, "requests.csv"));
        	} catch (IllegalStateException ise) {
        		log.log(Level.SEVERE, messages.getString("bot_not_registered"), ise);
        	} catch (IOException e) {
        		log.log(Level.SEVERE, messages.getString("csv_file_not_initialized"), e);
			}
        } catch (TelegramApiRequestException e) {
    		log.log(Level.SEVERE, messages.getString("no_internet"), e);
        }
	}


	@Override
	public void stop() throws Exception {
		session.stop();
	}


	@Override
	public void destroy() {
	}
	
}
