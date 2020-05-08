package org.tomvercaut.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommandStream extends Thread {
    private final InputStream is;
    private final String type;

    public CommandStream(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    @Override
    public void run() {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while((line=br.readLine())!= null) {
                System.out.println(type + "> " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
