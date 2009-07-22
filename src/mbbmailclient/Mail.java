/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mbbmailclient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ciw8062
 */
public class Mail {

    private String text;
    private boolean hasText;
    private String subject;
    private String from;
    private String time;
    private String header;
    private String messageid;
    private int id;
    private String headerLine;
    private boolean markedDeleted;

    public Mail (String header, int id) {
        hasText = false;
        markedDeleted = false;
        this.header = header;
        this.id = id;
        parseHeader(header);
    }

    public String getMessageId() {
        return messageid;
    }

    private void parseHeader(String header) {
        String lines[] = header.split("\n");
        for (int i=0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("^(.*?):(.*)");
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
                if (matcher.group(1).equals("From")) {
                    from = matcher.group(2);
                } else if (matcher.group(1).equalsIgnoreCase("Date")) {
                    time = matcher.group(2);
                } else if (matcher.group(1).equalsIgnoreCase("Subject")) {
                    subject = matcher.group(2);
                } else if (matcher.group(1).equalsIgnoreCase("Message-ID")) {
                    messageid = matcher.group(2);
                }
            }      
        }
    }

    public boolean setMail (String text) {
        //Check if messageid is the same for the mail we just got.
        String lines[] = text.split("\n");
        for (int i=0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("^(.*?):(.*)");
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
                if (matcher.group(1).equalsIgnoreCase("Message-ID")) {
                    if (!messageid.equalsIgnoreCase(matcher.group(2))) {
                        return false;
                    } else {
                        hasText = true;
                        this.text = text;
                    }
                    break;
                }
            }
        }

        return true;
    }

    public String getHeaderLine() {
        String deleted = "";
        String downloaded = "";
        if (hasText) {
           downloaded = "(!Downloaded!) ";
        }
        if (markedDeleted) {
            deleted = "(Marked as DELETED)";
        }
        return deleted + downloaded + "Date: " + time + " From: " + from + " Subject: " + subject;
    }
    public String getText() {
        return this.text;
    }

    public int getId() {
        return id;
    }
    
    public boolean alreadyDownloaded() {
        return hasText;
    }

    public void markDeleted(boolean yesno) {
        markedDeleted = yesno;
    }


    public boolean isMarkedAsDeleted() {
        return markedDeleted;
    }
}
