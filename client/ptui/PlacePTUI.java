package place.client.ptui;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;
import place.network.NetworkClient;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;
import java.util.Scanner;

import static java.lang.Thread.sleep;

/**
 * PlacePTUI is the plain text UI for Place
 *
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 */
public class PlacePTUI extends ConsoleApplication implements Observer<ClientModel, PlaceTile> {

    private ClientModel clientModel;
    private Scanner in;
    private String userName;
    private NetworkClient networkClient;

    /**
     * Calls private refresh method to get an updated board
     * and ask for input
     * @param model
     * @param tile
     */
    @Override
    public void update(ClientModel model, PlaceTile tile) {
        this.refresh();
    }

    /**
     * Refreshes board and asks for input
     */
    private void refresh() {

        final Reader reader = new InputStreamReader(System.in);
        //Used a InputStreamReader to only get input from keyboard when there is something present
        in = new Scanner(reader);
        System.out.println("\n" + clientModel.getBoard());
        System.out.println("\nMake a move: ");

        try {
            if(reader.ready()) {
                System.out.println("this happened");
                String moves[] = in.nextLine().split(" ");
                if(Integer.parseInt(moves[0]) != -1) {

                    networkClient.changeTile(
                            new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE,
                                    new PlaceTile(Integer.parseInt(moves[0]),
                                            Integer.parseInt(moves[1]),
                                            userName, PlaceColor.values()[Integer.parseInt(moves[2])])));

                    sleep(500);
                }
                else {
                    networkClient.close();
                    System.out.println("Client Disconnected");
                    System.exit(1);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * initializes the ClientModel and
     * utilizes the arguments to create NetworkClient
     * which connects to the server
     */
    public void init(){
        try {
            List< String > args = super.getArguments();

            String hostName = args.get(0);
            int port = Integer.parseInt(args.get(1));
            userName = args.get(2);

            clientModel = new ClientModel();
            networkClient = new NetworkClient(hostName, port, clientModel, userName);
            networkClient.connect();

        } catch (PlaceException e) {
            e.printStackTrace();
        }

    }

    /**
     * Gets input and output from ConsoleApplication,
     * waits for user to disconnect
     * @param userIn Scanner for System.in
     * @param userOut PrintWriter to the console
     */

    public synchronized void go(Scanner userIn, PrintWriter userOut){

        this.in = userIn;
        this.clientModel.addObserver(this);
        this.networkClient.startListener();

        this.refresh();
        while(true) {
            try {
                this.wait();
            }
            catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

    }

    /**
     * Checks for 3 arguments to be present
     * Launches the PTUI
     * @param args hostname port username
     */

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceClient host port username");
        }
        else {
            ConsoleApplication.launch(PlacePTUI.class, args);
        }

    }

}

