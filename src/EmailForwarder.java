import java.io.Console;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

public class EmailForwarder {

	// sender and receiver email addresses
	private static String gmxUsername = "";
	private static String gmxPassword = "";
	private static String gmailUsername = "";
	private static boolean isRunning = true;
	private static Console console = System.console();
	private static Session sendSession = null;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Perform cleanup or any desired action here
				System.out.println("Caught Ctrl+C - Performing cleanup...");
				sendLastMail();
				System.out.println("Exiting ...");
			}
		});

		readGMXUsername();
		readPasswordFromUser();
		readGmailUsername();

		// set properties to receive emails
		Properties properties = new Properties();
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.host", "mail.gmx.net");
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.user", gmxUsername);
		properties.put("mail.smtp.password", gmxPassword);
		properties.put("mail.smtp.starttls.enable", "true");

		// define a session to send emails
		Session sendSession = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(properties.getProperty("mail.smtp.user"),
						properties.getProperty("mail.smtp.password"));
			}
		});

		// properties to list emails
		Properties gmxProps = new Properties();
		gmxProps.put("mail.imap.host", "imap.gmx.com");
		gmxProps.put("mail.imap.port", "993");
		gmxProps.put("mail.imap.ssl.enable", "true");

		// session to receive emails
		Session receiveSession = Session.getInstance(gmxProps, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(gmxUsername, gmxPassword);
			}
		});

		try {
			// Store für die GMX-Verbindung öffnen
			Store gmxStore = receiveSession.getStore("imaps");
			gmxStore.connect("imap.gmx.com", gmxUsername, gmxPassword);

			// Ordner für die GMX-Verbindung öffnen
			Folder gmxFolder = gmxStore.getFolder("INBOX");
			gmxFolder.open(Folder.READ_WRITE);

			System.out.println("Running ...");
			while (isRunning == true) {
				Thread.sleep(10000);

				// get e-mails from the INBOX list
				Message[] messages = gmxFolder.getMessages();

				for (Message message : messages) {
					System.out.println("New -Mail with subject: " + message.getSubject());

					if (message.getSubject().contains("Ein Brief kommt in Kürze bei Ihnen an")) {
						// copy email content
						Message forwardedMessage = new MimeMessage(sendSession);
						forwardedMessage.setFrom(new InternetAddress(gmxUsername));
						forwardedMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(gmailUsername));
						forwardedMessage.setSubject(message.getSubject());
						forwardedMessage.setText(message.getContent().toString());
						forwardedMessage.setContent((Multipart) message.getContent());

						Transport.send(forwardedMessage);
						System.out.println("E-Mail forwarded: " + message.getSubject());
					}

					// delete from
					message.setFlag(Flags.Flag.DELETED, true);
					System.out.println("Deleted E-Mail with subject: " + message.getSubject());

					// update directory
					gmxFolder.expunge();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendLastMail() {
		try {
			if (sendSession != null) {
				Message forwardedMessage = new MimeMessage(sendSession);
				forwardedMessage.setFrom(new InternetAddress(gmxUsername));

				forwardedMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(gmailUsername));
				forwardedMessage.setSubject("E-Mail Forwarder Exited");

				Transport.send(forwardedMessage);
			}
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	private static boolean readGMXUsername() {
		try {
			System.out.print("Enter a GMX email address: ");
			gmxUsername = console.readLine();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private static boolean readGmailUsername() {
		try {
			System.out.print("Enter a Gmail email address: ");
			gmailUsername = console.readLine();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private static boolean readPasswordFromUser() {
		Console console = System.console();
		if (console == null) {
			System.out.println("No console available!");
			return false;
		} else {
			gmxPassword = new String(console.readPassword("Enter the password: "));
			return true;
		}
	}

}
