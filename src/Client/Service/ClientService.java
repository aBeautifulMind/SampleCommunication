package Client.Service;

/**
 * Created by Emanuele on 09/05/2016.
 */
 public abstract class ClientService {

    public abstract void SendMessage(String message);

    public abstract boolean Connect();
}
