/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mbbmailclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author werner
 */
public class SocketHandler extends Thread  {
    private String hostname;
    private int port;
    private Socket link;
    private Model model;
    private BufferedReader in;
    private PrintWriter out;
    private boolean expectingOutput;
    private boolean multiLine;
    private String expect;
    private String fullresponse;
    private boolean boolresponse;
    private Timer timer;

    SocketHandler(Model m, String hostname, int port) {
        model = m;
        this.hostname = hostname;
        this.port = port;
        link = null;
		try
		{
            link = new Socket();
            link.bind(null);
            //Connecten met timeout.
            link.connect(new InetSocketAddress(hostname, port), 500);
			in = new BufferedReader
					(new InputStreamReader
					   	(link.getInputStream()));//Step 2.
            out = new PrintWriter(
					link.getOutputStream(),true);	 //Step 2.
            listenTo("+OK");
		}
        catch(SocketTimeoutException e)
        {
            this.close();
        }
        catch(IOException e)
		{
			e.printStackTrace();
		}
    }
    
    public void close () {
        try {
            timer.cancel();
            link.close();
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void noopTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new noopTask(), 20000); //over 20 seconden
    }

    class noopTask extends TimerTask {
        public void run() {
            SocketHandler.this.sendCommand("NOOP", "+OK", false);
        }
    }

    @Override
    public void run() {
        String response = "";
        boolean goodresponse = true;
        while (expectingOutput) {
            try {
                String line = in.readLine();
                
                if (multiLine == false) {
                    model.addStatusLine(line);
                    if (line.indexOf(expect) != -1) {
                        setResponse(line, true);
                    } else {
                        setResponse(line, false);
                    }
                    expectingOutput = false;
                    new Thread(model).start();
                    break;
                } else {
                    if (response.equals("") && (line.indexOf(expect) == -1)) {
                        goodresponse = false;
                        model.addStatusLine("" + line);
                    } else {
                        model.addStatusLine(line);
                    }
                    response += line + "\n";
                    if (line.equals(".")) {
                        setResponse(response, goodresponse);
                        expectingOutput = false;
                        new Thread(model).start();
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public synchronized void listenTo(String s) {
        listenTo(s, false);
    }

    public synchronized void listenTo(String s, boolean multiline) {
        if (expectingOutput == true) {
            System.err.println("Can't listen to more than one response at a time!");
        } else {
            expectingOutput = true;
            this.multiLine = multiline;
            expect = s;
            //this.start();
            new Thread(this).start();
        }
    }

    public synchronized void setResponse(String s, boolean found) {
        fullresponse = s;
        boolresponse = found;
    }

    public synchronized String getTextResponse() {
        return fullresponse;
    }

    public synchronized boolean getBoolResponse() {
        return boolresponse;
    }

    public synchronized void sendCommand(String s, String expect, boolean multiline) {
        out.println(s);
        listenTo(expect, multiline);
        model.addStatusLine("GREENCOLOR:" + s);
        noopTimer();
    }
}
