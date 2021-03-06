package ClientPackage.View.GUIResources.Class;

import ClientPackage.Controller.ClientController;
import ClientPackage.View.GeneralView.GUIView;
import CommonModel.GameModel.City.City;
import CommonModel.Snapshot.SnapshotToSend;

import java.util.ArrayList;

/**
 * Created by Giulio on 28/05/2016.
 */
public interface BaseController {

    void updateView();

    void setClientController(ClientController clientController, GUIView guiView);

    void setMyTurn(boolean myTurn, SnapshotToSend snapshot);

    void onStartMarket();

    void onStartBuyPhase();

    void onFinishMarket();

    void selectPermitCard();

    void selectCityRewardBonus();

    void moveKing(ArrayList<City> kingPath);

    void selectOldPermitCardBonus();

}
