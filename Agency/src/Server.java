

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

    Database database = new Database();

    private ServerSocket listener;
    private Socket client;
    private boolean running;

    private void init() {

        database.init();

        running = true;
        try {
            listener = new ServerSocket(3434);
            System.out.println("SERVER STARTED");
            listen();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rebuild() {
        FileReader fileReader = new FileReader();
        database.rebuild();
        fileReader.passDatabaseReference(database);
        fileReader.readCustomerFile();
        fileReader.readVehicleFile();
        fileReader.readRentalFile();
    }

    private void listen() {
        try {
            client = listener.accept();
            while (running) {
                processClient();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processClient() {

        try {
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            try {
                String request = (String) in.readObject();

                if (request.equalsIgnoreCase("CLOSE")) {
                    out.writeObject(request);
                    out.flush();
                    System.out.println("SERVER WILL NOW CLOSE...");
                    running = false;
                    database.close();
                    out.close();
                    in.close();
                    client.close();
                    listener.close();
                }
                else if (request.equalsIgnoreCase("REBUILD")) {
                    rebuild();
                    out.writeObject("DATABASE RELOADED");
                    out.flush();
                }
                else {

                    char type = request.charAt(0);
                    char table = request.charAt(1);
                    String sql = request.substring(2);

                    if (type == 'Q') {
                        ArrayList<String> results = (ArrayList<String>) database.executeQuery(table, sql);
                        out.writeObject(results);
                        out.flush();
                    }
                    else if (type == 'U') {
                        String result;
                        if (table == 'R') {
                            String parts[] = sql.split("#");
                            result = database.executeUpdate(parts[0], parts[1], parts[2]);
                        }
                        else {
                            result = database.executeUpdate(sql);
                        }
                        out.writeObject(result);
                        out.flush();
                    }
                }
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server().init();
    }
}