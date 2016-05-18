package ClientPackage.NetworkInterface;

import ClientPackage.Controller.ClientController;
import Utilities.Class.Constants;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Emanuele on 09/05/2016.
 */
public class ClientFactoryService {

    public static ClientService getService(String method, String serverIP, ClientController clientController) throws RemoteException, NotBoundException {
        if(method.equals(Constants.RMI)) {
            return new ClientRMIService(Constants.SERVER, serverIP, clientController);
        }
        else{
            return new ClientSocketService(serverIP, clientController);
        }
    }
}
