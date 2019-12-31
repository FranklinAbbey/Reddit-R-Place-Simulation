package place.network;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.model.ClientModel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * NetworkClient is the client side interface for the Place Server
 * Represents the controller part of the a MVC
 * Forwards user actions to the server it's connected to
 *
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 */

public class NetworkClient {

    /**
     * The ObjectInputStream receives PlaceRequests from the server
     */
    private ObjectInputStream networkIn;
    /**
     * The ObjectOutputStream sends PlaceRequests to the server
     */
    private ObjectOutputStream networkOut;
    /**
     * The ClientModel keeps track of the current PlaceBoard
     */
    private ClientModel clientModel;
    /**
     * Go allows the NetworkClient's Listener to continue receiving
     * PlaceRequests from the server
     */
    private boolean go;

    /**
     * Returns the state of go
     * @return - boolean 'go'
     */
    private boolean goodToGo(){return go;}

    /**
     * FLips go to false when we want the Listener to stop
     */
    private void stop() {go = false;}

    /**
     * Connects to a PlaceBoard server
     * Afterwards a Listener thread forwards updates to the ClientModel
     * @param hostname the name of the host running the server
     * @param port the port of the server socket on which the server is running
     * @param model the object that is holding the current state of the board
     * @param userName the username of the client that is connected to the server
     * @throws PlaceException If there's a problem connecting
     */
    public NetworkClient( String hostname, int port, ClientModel model, String userName) throws PlaceException {
        try {
            Socket socket = new Socket(hostname, port);
            this.networkIn = new ObjectInputStream( socket.getInputStream() );
            this.networkOut = new ObjectOutputStream( socket.getOutputStream() );
            this.clientModel = model;
            this.go = true;

            // send login to server
            this.networkOut.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, userName));
            this.networkOut.flush();

            // make sure login was success from server
            try {
                PlaceRequest initial = (PlaceRequest)networkIn.readUnshared();
                if (initial.getType().equals(PlaceRequest.RequestType.LOGIN_SUCCESS)) {
                    System.out.println("Successful login: " + hostname + " " + port);
                }
                else if(initial.getType().equals(PlaceRequest.RequestType.ERROR)) {
                    System.out.println("Username \"" + userName + "\" taken");
                    System.exit(1);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        catch( IOException e ) {
            throw new PlaceException( e );
        }
    }

    public void startListener() {
        // Run rest of client in separate thread.
        // This threads stops on its own at the end of the game and
        // does not need to rendezvous with other software components.
        Thread netThread = new Thread( () -> this.run() );
        netThread.start();
    }

    /**
     * Receives board from the server and sends initial state of
     * the board to the ClientModel
     */
    public void connect(){
        // receive Board from Server
        try {
            PlaceRequest board = (PlaceRequest) networkIn.readUnshared();
            clientModel.setBoard((PlaceBoard)board.getData());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * The Listener Thread uses this method to continually
     * listen to messages sent from the server
     * stops on errors and exceptions
     */
    private void run() {

        while (this.goodToGo()) {
            try {
                PlaceRequest<?> request = (PlaceRequest)networkIn.readUnshared();

                switch ( request.getType() ) {
                    case TILE_CHANGED:
                        clientModel.changeTile((PlaceTile)request.getData());
                        break;
                    case ERROR:
                        System.out.println(request.getData());
                        stop();
                        break;

                    default:
                        System.err
                                .println( "Unrecognized request: " + request );
                        this.stop();
                        break;
                }
            }
            catch( NoSuchElementException nse ) {
                // Looks like the connection shut down.
                System.out.println(nse.getMessage());
                this.stop();
            }
            catch( Exception e ) {
                System.out.println( e.getMessage() + '?' );
                this.stop();
            }
        }
        this.close();
    }

    /**
     * Sends a request to the server including the
     * of changeTile and a given PlaceTile
     * @param request request to be sent to the server
     */
    public void changeTile(PlaceRequest request){
        try {
            networkOut.writeUnshared(request);
            networkOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the current server connections
     * because of an error in the server or exception in
     * the NetworkClient
     */
    public void close() {

        try {
            networkOut.close();
            networkIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
