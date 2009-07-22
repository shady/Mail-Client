/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mbbmailclient;

import java.util.Iterator;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 *
 * @author ciw8062
 */
public class Model extends Observable implements Runnable {
    private String hostname;
    private int port;
    private String user;
    private String pass;
    private Vector<String> statuslog;
    private int connectionalive;
    private Vector<Mail> mailVector;
    private Vector<String> headerList;
    private Mail currentMail;
    private SocketHandler con;
    private String currentStage;
    private int currentPhase;
    private int statReturn;
    private int headerDone;
    private int handlingMailID;
    private boolean needSettings;

    public Model() {
        needSettings = true;
        statuslog = new Vector<String>();
        mailVector = new Vector<Mail>();
        headerList = new Vector<String>();
        connectionalive = 0;
    }

    public void setHostName(String s) {
        hostname = s;
    }
    public void setPort(int p) {
        port = p;
    }
    public void setUser(String s) {
        user = s;
    }
    public void setPass(String s) {
        pass = s;
    }

    public String getHostName() {
        return hostname;
    }
    public int getPort() {
        return port;
    }
    public String getUser() {
        return user;
    }
    public String getPass() {
        return pass;
    }

    /**
     * Executed as a thread. When stage is accessed it means the server returned the right response.
     */
    public void escalateStage()  {
        currentPhase++;
        if (currentStage.equals("connect")) {
            connect(currentPhase);
        } else if (currentStage.equals("login")) {
            login(currentPhase);
        } else if (currentStage.equals("close")) {
            close(currentPhase);
        } else if (currentStage.equals("stat")) {
            stat(currentPhase);
        } else if (currentStage.equals("top")) {
            top(currentPhase);
        } else if (currentStage.equals("retr")) {
            retr(currentPhase, 0);
        } else if (currentStage.equals("dele")) {
            dele(currentPhase, 0);
        } else if (currentStage.equals("connectThread")) {
            connect(0);
        }
    }

    public void connect() {
        currentStage = "connectThread";
        if (hostname.isEmpty() || port == 0 || user.isEmpty() || pass.isEmpty()) {
            addStatusLine("REDCOLOR:#One or more settings are missing.");
        } else {
            new Thread(this).start();
        }
    }

    public void connect(int phase) {
        if (phase == 0) {
            currentStage = "connect";
            currentPhase = 0;
            setConnectedStatus(1);
            con = new SocketHandler(this, hostname, port);
        } else if (phase == 1) {
            if (con.getBoolResponse() == false) {
                System.out.println("noconnect");
            }
            addStatusLine("BLUECOLOR:#Connected!");
            login(0);
        }
    }

    public void close(int phase) {
        if (phase == 0) {
            currentStage = "close";
            currentPhase = 0;
            setConnectedStatus(1);
            addStatusLine("BLUECOLOR:#Disconnecting");
            con.sendCommand("QUIT ", "+OK", false);
            mailVector.removeAllElements();
        } else if (phase == 1) {
            if (con.getBoolResponse() == true) {
                addStatusLine("BLUECOLOR:#Disconnected");
                con.close();
            } else {
                addStatusLine("REDCOLOR:#Not disconnected, Forcing close.");
                con.close();
            }
            setConnectedStatus(0);
            updateView(false);
        }
    }

    public void login(int phase) {
        if (phase == 0) {
            currentStage = "login";
            currentPhase = 0;
            //Send username
            addStatusLine("BLUECOLOR:#Sending username:");
            con.sendCommand("USER " + user, "+OK", false);
        } else if (phase == 1) {
            //Recieved response from USER command
            if (con.getBoolResponse() == false) {
                addStatusLine("REDCOLOR:#Niet ingelogd, fout username: " + user + "");
                setConnectedStatus(0);
            } else {
                addStatusLine("BLUECOLOR:#Sending Password:");
                con.sendCommand("PASS " + pass, "+OK Logged in.", false);
            }
        } else if (phase == 2) {
            //Recieved response from PASS command
            if (con.getBoolResponse() == false) {
                addStatusLine("REDCOLOR:#Niet ingelogd: fout password, " + pass + "");
                setConnectedStatus(0);
            } else {
                addStatusLine("BLUECOLOR:#Logged in!");
                setConnectedStatus(2);
            }
            
        }
    }

    
    public Vector getHeaderList() {
        headerList.removeAllElements();
        Iterator<Mail> it = mailVector.iterator();
        while (it.hasNext()) {
            headerList.add(it.next().getHeaderLine());
        }
        return headerList;
    }

    public void top(int phase) {
        if (phase == 0) {
            currentStage = "top";
            currentPhase = 0;
            headerDone = 0;
            addStatusLine("BLUECOLOR:#Fetching Headers");
            //mailVector.removeAllElements();
            stat(0);
        } else if (phase == 1) {
            currentStage = "top";
            currentPhase = 1;
            if (headerDone != statReturn) {
                con.sendCommand("TOP " + (headerDone+1) + " 0", "+OK", true);
            } else {
                top(3);
            }
        } else if (phase == 2) {
            headerDone++;
            if (con.getBoolResponse() == false) {
                addStatusLine("REDCOLOR:#Something went wrong fetching the header.");
                //return;
            }
            Mail mail = new Mail(con.getTextResponse(), (headerDone));

            Iterator<Mail> it = mailVector.iterator();
            boolean downloadaheady = true;
            while (it.hasNext() && downloadaheady == true) {
                if (it.next().getMessageId().equals(mail.getMessageId())) {
                    downloadaheady = false;
                }
            }

            if (downloadaheady == true) {
                mailVector.add(mail);
            } else {
                addStatusLine("REDCOLOR:#Already have this message. Not adding");
            }
            
            updateView(false);
            if (headerDone != statReturn) {
                top(1);
            } else {
                top(3);
            }
        } else if (phase == 3) {
            addStatusLine("BLUECOLOR:#Done fetching!");
        }
    }

    public void stat(int phase) {
        if (phase == 0) {
            currentStage = "stat";
            currentPhase = 0;
            addStatusLine("BLUECOLOR:#Getting number of messages.");
            con.sendCommand("STAT", "+OK", false);
        } else if (phase == 1) {
            int numNew = Integer.valueOf(con.getTextResponse().split(" ")[1]);
            addStatusLine("BLUECOLOR:#" + numNew + " emails found.");
            statReturn = numNew;
            top(1);
        }
    }

    public void rset(int phase) {
        if (phase == 0) {
            currentStage = "rset";
            currentPhase = 0;
            addStatusLine("BLUECOLOR:#Unmarking all messages as deleted!");
            con.sendCommand("RSET", "+OK", false);
        } else if (phase == 1) {
            addStatusLine("BLUECOLOR:#Undeleted all mails.");
            Iterator<Mail> it = mailVector.iterator();
            boolean downloadaheady = true;
            while (it.hasNext() && downloadaheady == true) {
                it.next().markDeleted(false);
            }
            updateView(false);
        }
    }

    public void dele(int phase, int mailNr) {
        if (phase == 0) {
            currentStage = "dele";
            currentPhase = 0;
            handlingMailID = mailNr + 1;
            addStatusLine("BLUECOLOR:#Marking Mail: " + handlingMailID + " as deleted.");
            con.sendCommand("DELE " + handlingMailID, "+OK", false);
        } else if (phase == 1) {
            if (con.getBoolResponse() == false) {
                addStatusLine("REDCOLOR:#No such message! " + handlingMailID);
            } else {
                addStatusLine("BLUECOLOR:#Marked as deleted: " + handlingMailID);
                mailVector.get(handlingMailID - 1).markDeleted(true);
                updateView(false);
            }
        }
    }

    private void retr(int phase, int mailNr) {
        if (phase == 0) {
            currentStage = "retr";
            currentPhase = 0;
            handlingMailID = mailNr;
            addStatusLine("BLUECOLOR:#Getting mail " + mailNr + "");
            con.sendCommand("RETR " + mailNr, "+OK", true);
        } else if (phase == 1) {
            if (con.getBoolResponse() == false) {
                addStatusLine("REDCOLOR:#Something went wrong during fetching.");
                close(0);
            } else {
                addStatusLine("BLUECOLOR:#Downloaded mail " + handlingMailID);
                mailVector.get(handlingMailID - 1).setMail(con.getTextResponse());
                updateView(false);
                openMail(handlingMailID - 1);
            }
        }
    }

    public void run() {
        escalateStage();
    }

    public void openMail(int index) {
        currentMail = mailVector.get(index);
        if (currentMail.alreadyDownloaded() == false) {
            System.out.println("tset");
            retr(0, (index+1));
        }
        updateView(false);
    }

    public void setConnectedStatus(int i) {
        connectionalive = i;
        updateView(false);
    }

    public int getConnectedStatus() {
        return connectionalive;
    }

    public synchronized void addStatusLine(String s) {
        statuslog.add(s);
        updateView(true);
    }

    public String getMail() {
        if (currentMail != null && currentMail.alreadyDownloaded()) {
            return currentMail.getText();
        } else {
            return "No mail.";
        }
    }

    public synchronized String getLatestLineStatusLog() {
        if (!statuslog.isEmpty()) {
            return statuslog.lastElement() + "\n";
        } else {
            return "";
        }
    }

    public void updateView(boolean onlyStatus) {
        setChanged();
        if (onlyStatus) {
            notifyObservers("Status");
        } else {
            notifyObservers("All");
        }
    }
}
