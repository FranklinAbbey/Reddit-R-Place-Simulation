package place.model;

import place.PlaceBoard;
import place.PlaceTile;
import place.client.ptui.PlacePTUI;

import java.util.LinkedList;
import java.util.List;

/**
 * The client side model that is used as the "M" in the MVC paradigm.  All client
 * side applications (PTUI, GUI, bots) are observers of this model.
 *
 * @author Sean Strout @ RIT CS
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 */
public class ClientModel {
    /** the actual board that holds the tiles */
    private PlaceBoard board;

    /** observers of the model (PlacePTUI and PlaceGUI - the "views") */
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    /**
     * Add a new observer.
     *
     * @param observer the new observer
     */
    public void addObserver(Observer<ClientModel, PlaceTile> observer) {
        this.observers.add(observer);
    }

    /**
     * Notify observers the model has changed.
     */
    private void notifyObservers(PlaceTile tile){
        for (Observer<ClientModel, PlaceTile> observer: observers) {
            observer.update(this, tile);
        }
    }

    /**
     * Sets initial Board
     * @param boardSent board to be set
     */
    public void setBoard(PlaceBoard boardSent) {
        this.board = boardSent;
        notifyObservers(null);

    }

    /**
     * Changes a tile if it's a valid move
     * and notifies the PTUI
     * @param tile tile to be changed
     */
    public void changeTile(PlaceTile tile) {
        if(board.isValid(tile)) {
            this.board.setTile(tile);
            notifyObservers(tile);
        }
        else {
            System.out.println("Invalid Move!");
            System.exit(1);
        }
    }

    /**
     * Gets the Client Model's Board
     * @return the client model's board
     */
    public PlaceBoard getBoard() {
        return board;
    }

}
