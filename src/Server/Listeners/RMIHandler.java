package Server.Listeners;

import Interface.RMIClientHandler;
import Server.Communication.BaseCommunication;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Emanuele on 09/05/2016.
 */
public class RMIHandler extends BaseCommunication implements RMIClientHandler {

    protected RMIHandler(String name) throws RemoteException {
        //super();
        UnicastRemoteObject.exportObject(this,0);
    }

    @Override
    public String sayHello() {
        System.out.println("Say hello");
        return "Hello!";
    }

    @Override
    public void sendMessage(String message) {

    }
}
