package place.server;

import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.Date;

/**
 * Represents a client connection as a Thread for
 * a PlaceServer to handle
 *
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 *
 */
public class ServerThread extends Thread{

    private String username;
    private ObjectInputStream networkIn;
    private ObjectOutputStream networkOut;
    private PlaceServer server;
    private long bornTime;

    /**
     * create a new ServerThread, and read the initial LOGIN request
     *
     * @param out ObjectOutputStream - the client's output stream
     * @param in - ObjectInputStream - the client's input stream
     * @param s PlaceServer - the server that is handling this client
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public ServerThread(ObjectOutputStream out, ObjectInputStream in, PlaceServer s) throws IOException, ClassNotFoundException {
        networkIn = in;
        networkOut = out;
        server = s;
        // store the time of creation to prevent thread bombing
        bornTime = new Date().getTime();
        PlaceRequest request = (PlaceRequest) networkIn.readUnshared();
        if(request.getType() == PlaceRequest.RequestType.LOGIN){
            username = (String) request.getData();
            System.out.println(" username: " + username);
        }
    }

    /**
     * executes when the Thread is started. If this point is reached,
     * the client has successfully connected, and the Thread will send
     * the proper protocol message and the PlaceBoard. Then it will
     * read PlaceTile changes until the client disconnects
     *
     */
    @Override
    public void run() {
        boolean flag = true;
        try {
            networkOut.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, null));
            networkOut.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.BOARD, server.getBoard()));
            System.out.println("Success!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (flag) {
            try {
                // read a request from the client
                PlaceRequest<?> request = (PlaceRequest) networkIn.readUnshared();
                // if a PlaceTile was changed
                if(request.getType() == PlaceRequest.RequestType.CHANGE_TILE) {
                        PlaceTile tile = (PlaceTile) request.getData();
                        tile.setTime(new Date().getTime());
                        // try to make the move, if the move is invalid send an ERROR
                        if(!server.changeTile(tile)) {
                            networkOut.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Invalid Placement"));
                        }
                        // otherwise the move was valid, make the thread sleep to delay moves
                        else {
                            System.out.println(username + " updated board");
                            sleep(500);
                        }
                }
            }
            // caught when Client disconnects
            catch(SocketException e) {
                server.removeUser(this);
                flag = false;
            }

            catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * retrieve the Thread's time of creation
     *
     * @return long bornTime - the time the Thread was created
     */
    public long getBornTime(){return bornTime;}

    /**
     * retrieve the Thread's userName
     *
     * @return String username
     */
    public String getUsername(){return username;}

    /**
     * retrieve the Thread's output stream
     *
     * @return ObjectOutputStream networkOut
     */
    public ObjectOutputStream getNetworkOut(){return networkOut;}

    /**
     * retrieve the Thread's input stream
     *
     * @return ObjectInputStream networkIn
     */
    public ObjectInputStream getNetworkIn(){return networkIn;}

}
