package ClientPackage.NetworkInterface;

import ClientPackage.Controller.ClientController;
import CommonModel.GameModel.Action.Action;
import CommonModel.GameModel.Card.SingleCard.PermitCard.PermitCard;
import CommonModel.GameModel.City.City;
import CommonModel.GameModel.Market.BuyableWrapper;
import CommonModel.Snapshot.BaseUser;
import CommonModel.Snapshot.SnapshotToSend;
import RMIInterface.RMIClientHandler;
import RMIInterface.RMIClientInterface;
import RMIInterface.RMIListenerInterface;
import Server.Model.Map;
import Utilities.Class.Constants;
import Utilities.Exception.ActionNotPossibleException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** This is the class that represents clients in the communication took place via RMI.
 * Created by Emanuele on 09/05/2016.
 */
public class ClientRMIService extends ClientService implements RMIClientInterface {

    private String serverName;
    private RMIListenerInterface rmiListenerInterface;
    private Registry registry;
    private String rmiHandlerName;
    private RMIClientHandler rmiClientHandler;
    private ClientController clientController;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    ClientRMIService(String serverName, String serverIP, ClientController clientController) throws RemoteException, NotBoundException {
        this.serverName = serverName;
        this.clientController = clientController;
        registry = LocateRegistry.getRegistry(serverIP, Constants.RMI_PORT);
        rmiListenerInterface = (RMIListenerInterface) registry.lookup(serverName);
        UnicastRemoteObject.exportObject(this, 0);
    }

    @Override
    public boolean Connect() {
        try {
            rmiHandlerName = rmiListenerInterface.Connect();
            rmiClientHandler = (RMIClientHandler) registry.lookup(rmiHandlerName);
            rmiClientHandler.sendRemoteClientObject(this);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void sendName(String name) {
        Runnable runnable = () -> {
            try {
                boolean result = rmiClientHandler.tryToSetName(name);
                clientController.onNameReceived(result);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onAction(Action action) throws ActionNotPossibleException, RemoteException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    rmiClientHandler.action(action);
                } catch (ActionNotPossibleException e) {
                    clientController.onActionNotPossible(e);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void sendMap(Map map) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.sendMap(map);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void sendSaleItem(ArrayList<BuyableWrapper> realSaleList) {
        Runnable runnable = () -> {
            try {
                if (rmiClientHandler.sendBuyableObject(realSaleList)) {
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onBuy(ArrayList<BuyableWrapper> buyList) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.buyObject(buyList);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onRemoveItemFromMarket(BuyableWrapper item) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.onRemoveItem(item);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onFinishSellPhase() {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.onFinishSellPhase();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void sendFinishedBuyPhase() {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.onFinishBuyPhase();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void getCityRewardBonus(City city1) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.getCityRewardBonus(city1);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ActionNotPossibleException e) {
                clientController.onActionNotPossible(e);
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onSelectPermitCard(PermitCard permitCard) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.onSelectPermitCard(permitCard);
            } catch (RemoteException e) {

            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onFinishTurn() {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.finishRound();
            } catch (RemoteException e) {

            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void onSelectOldPermitCard(PermitCard permitCard) {
        Runnable runnable = () -> {
            try {
                rmiClientHandler.onSelectOldPermitCard(permitCard);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };
        executorService.execute(runnable);
    }

    @Override
    public void sendSnapshot(SnapshotToSend snapshotToSend) throws RemoteException {
        Runnable runnable = () -> {
            clientController.setSnapshot(snapshotToSend);
        };
        executorService.execute(runnable);
    }

    @Override
    public void sendMap(ArrayList<Map> mapArrayList) {
        Runnable runnable = () -> {
            clientController.showMap(mapArrayList);
        };
        executorService.execute(runnable);
    }

    @Override
    public void gameInitialization(SnapshotToSend snapshotToSend) throws RemoteException {
        executorService.execute(() -> {
            clientController.gameInitialization(snapshotToSend);
        });

    }

    @Override
    public void isYourTurn() throws RemoteException {
        executorService.execute(() -> {
            clientController.isMyTurn();
        });

    }

    @Override
    public void finishTurn() throws RemoteException {
        executorService.execute(() -> {
            clientController.turnFinished();
        });

    }

    @Override
    public void onStartMarket() throws RemoteException {
        executorService.execute(() -> {
            clientController.onStartMarket();
        });

    }

    @Override
    public void onStartBuyPhase() throws RemoteException {
        executorService.execute(() -> {
            clientController.onStartBuyPhase();
        });

    }

    @Override
    public void disableMarketPhase() throws RemoteException {
        executorService.execute(() -> {
            clientController.onFinishBuyPhase();
        });
    }

    @Override
    public void selectPermitCard() throws RemoteException {
        executorService.execute(() -> {
            clientController.selectPermitCard();
        });

    }

    @Override
    public void selectCityRewardBonus(SnapshotToSend snapshotToSend) throws RemoteException {
        executorService.execute(() -> {
            clientController.selectCityRewardBonus(snapshotToSend);
        });
    }

    @Override
    public void moveKing(ArrayList<City> kingPath) throws RemoteException {
        executorService.execute(() -> {
            clientController.onMoveKing(kingPath);
        });
    }

    @Override
    public void sendMatchFinishedWithWin(ArrayList<BaseUser> finalSnapshot) throws RemoteException {
        executorService.execute(() -> {
            clientController.sendMatchFinishedWithWin(finalSnapshot);
        });
    }

    @Override
    public void ping() throws RemoteException {
    }

    @Override
    public void selectOldPermiCard() throws RemoteException {
        executorService.execute(() -> {
            clientController.selectOldPermitCardBonus();
        });

    }

    @Override
    public void onUserDisconnect(String username) throws RemoteException {
        executorService.execute(() -> {
            clientController.onUserDisconnect(username);
        });
    }
}
