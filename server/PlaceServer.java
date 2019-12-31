package place.server;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * The Place server is run on the command line as:
 *
 * $ java PlaceServer port DIM
 *
 * Where port is the port number of the host and DIM is the square dimension
 * of the board.
 *
 * @author Sean Strout @ RIT CS
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 *
 */
public class PlaceServer implements Closeable {

    private ServerSocket serverSocket;
    private List<ServerThread> connections = new ArrayList<>();
    private PlaceBoard board;
    private Set<InetAddress> IPs = new HashSet<>();
    // used for Thread bombing
    private final int connectionDelay = 500;

    /**
     * constructor for PlaceSever which creates the Server Socket
     *
     * @param port - the connection port
     * @param dim - the dimensions of the PlaceBoard
     * @throws PlaceException
     */
    private PlaceServer(int port, int dim) throws PlaceException {
        try {
            this.serverSocket = new ServerSocket(port);
            board = new PlaceBoard(dim);
        } catch (IOException e) {
            throw new PlaceException(e);
        }
    }

    /**
     *  allow clients to connect one at a time, create a new ServerThread
     *  object to deal with this connection, and start the thread as long as
     *  parameters are met:
     *      1. The username is not taken
     *      2. The IP address hasn't recently connected (thread bombing)
     *
     * @param s - PlaceServer object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void startListener(PlaceServer s) throws IOException, ClassNotFoundException {
        // Run rest of client in separate thread.
        // This threads stops on its own at the end of the game and
        // does not need to rendezvous with other software components.
        Socket clientSocket = s.serverSocket.accept();
        System.out.print("Client connecting... \n IP: " + clientSocket.getInetAddress() + "\n port: " + clientSocket.getLocalPort() + "\n");
        IPs.add(clientSocket.getInetAddress());
        ServerThread netThread = new ServerThread(new ObjectOutputStream(clientSocket.getOutputStream()), new ObjectInputStream(clientSocket.getInputStream()), s);
        boolean flag = false, flag2 = false;
        for (ServerThread current : connections) {
            // if the username is already connected
            if (current.getUsername().equals(netThread.getUsername())) {
                flag = true;
                break;
            }
            // checking if the IP has connected in the last second
            else if (current.getBornTime() + connectionDelay > new Date().getTime()){
                if(IPs.contains(clientSocket.getInetAddress())){
                    flag = true;
                    flag2 = true;
                    break;
                }
            }
        }
        // if the username isn't already used, and the IP hasn't recently connected
        if(!flag){
            connections.add(netThread);
            netThread.start();
        } else {
            netThread.getNetworkOut().writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, null));
            System.out.println("Failed to connect: ");
            // customized error messages for Server
            if(!flag2){
                System.out.println(" User's name was taken");
            }
            else{
                System.out.println(" IP Address is connecting too quickly");
            }
        }
    }

    /**
     * remove a user from the server once they disconnect
     *
     * @param userThread - the user to be removed, represented as a ServerThread
     */
    public synchronized void removeUser(ServerThread userThread) {
        connections.remove(userThread);
        System.out.println("Removed user: " + userThread.getUsername());
        System.out.println("Current connected users: ");
        for (ServerThread current : connections) {
            System.out.print(current.getUsername() + " ");
        }
        System.out.println();
    }

    /**
     *
     * @param tile - the tile to be changed
     * @return boolean - true: the move was valid and the tile has been changed
     *                   false: the move was invalid, and no tile was changed
     * @throws IOException
     */
    public synchronized boolean changeTile(PlaceTile tile) throws IOException {
        if(board.isValid(tile)){
            board.setTile(tile);
            for (ServerThread current: connections) {
                current.getNetworkOut().writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile));
            }
            return true;
        }
        return false;
    }

    /**
     * Closes the client {@link Socket}.
     */
    @Override
    public void close() {
        try {
            this.serverSocket.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    /**
     *  access the PlaceBoard object
     *
     * @return PlaceBoard - the board object
     */
    public PlaceBoard getBoard(){
        return board;
    }

    /**
     * The main method starts the server and spawns client threads each time a new
     * client connects.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java PlaceServer port DIM");
            System.exit(1);
        }

        try (PlaceServer server = new PlaceServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]))) {
            while(true) {
                server.startListener(server);
            }

        } catch (PlaceException e) {
            System.err.println("Failed to start server!");
            System.out.println(e.getMessage());
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}