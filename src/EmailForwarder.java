import java.io.Console;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * @author Valentino
 * 
 *         The Deutsche Post Email Forwarder is a program that automates the
 *         process of forwarding Deutsche Post emails regarding
 *         "Briefankündigung" to desired email addresses. This program is
 *         specifically designed for users with GMX email accounts. *
 */
public class EmailForwarder {

	// sender and receiver email addresses
	// set credentials to run the program without user input
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
				// perform cleanup or any desired action here
				System.out.println("Caught Ctrl+C - Performing cleanup...");
				sendLastMail();
				System.out.println("Exiting ...");
				isRunning = false;
			}
		});

		// ####################################################################
		// if you want to run this program without any user input
		// comment following three lines and set the credentials on top
		// ####################################################################
		if (gmxUsername.equals("") && gmxPassword.equals("") && gmailUsername.equals("")) {
			// checking the password as well, as no email provider allows empty passwords
			// anyway.
			readGMXUsername();
			readPasswordFromUser();
			readGmailUsername();
		} else {
			System.out.println("Credentials set already, skipping user input.");
		}
		
		// set properties to receive emails
		Properties properties = new Properties();
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.host", "mail.gmx.net");
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.user", gmxUsername);
		properties.put("mail.smtp.password", gmxPassword);
		properties.put("mail.smtp.starttls.enable", "true");

		while (isRunning) {
			try {
				Thread.sleep(10000);

				System.out.println("Trying to connect ...");
				
				// define a session to send emails
				sendSession = Session.getInstance(properties, new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(properties.getProperty("mail.smtp.user"),
								properties.getProperty("mail.smtp.password"));
					}
				});

				// properties to list emails for gmx email address
				Properties gmxProps = new Properties();
				gmxProps.put("mail.imap.host", "imap.gmx.com");
				gmxProps.put("mail.imap.port", "993");
				gmxProps.put("mail.imap.ssl.enable", "true");

				// create session to receive gmx emails
				Session receiveSession = Session.getInstance(gmxProps, new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(gmxUsername, gmxPassword);
					}
				});

				// open store for gmx
				Store gmxStore = receiveSession.getStore("imaps");
				gmxStore.connect("imap.gmx.com", gmxUsername, gmxPassword);

				// open directory for gmx email (inbox)
				Folder gmxFolder = gmxStore.getFolder("INBOX");
				gmxFolder.open(Folder.READ_WRITE);

				System.out.println("Running ...");

				// using this construction, to prevent the program to exit on exception.
				// indead of exiting it tries to reopen the directory.
				while (isRunning == true) {
					// get e-mails from the INBOX list
					Message[] messages;
					messages = gmxFolder.getMessages();

					if (messages != null)
						for (Message message : messages) {
							System.out.println("New mail with subject: " + message.getSubject());

							if (message.getSubject().contains("Ein Brief kommt in Kürze bei Ihnen an")) {
								// copy email content
								Message forwardedMessage = new MimeMessage(sendSession);
								forwardedMessage.setFrom(new InternetAddress(gmxUsername));
								forwardedMessage.setRecipients(Message.RecipientType.TO,
										InternetAddress.parse(gmailUsername));
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

					// sleep one minute
					Thread.sleep(1000 * 60);
				}

			} catch (FolderClosedException fce) {
				// ignore this exception, as this occurs every day, at least one time.
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * send last mail to the receiver, before final close
	 */
	private static void sendLastMail() {
		try {
			if (sendSession != null) {
				Message forwardedMessage = new MimeMessage(sendSession);
				forwardedMessage.setFrom(new InternetAddress(gmxUsername));

				forwardedMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(gmailUsername));
				forwardedMessage.setSubject("E-Mail Forwarder Exited");
				forwardedMessage.setText("restart me");
				Transport.send(forwardedMessage);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * reads in the gmx username
	 * 
	 * @return true if successful
	 */
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

	/**
	 * reads in the gmail username
	 * 
	 * @return true if successful
	 */
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

	/**
	 * reads in the gmx password (not visible on console)
	 * 
	 * @return true if successful
	 */
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
